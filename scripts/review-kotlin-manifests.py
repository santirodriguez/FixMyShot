#!/usr/bin/env python3
"""Append exact Kotlin files proved by separately pinned official release manifests."""
import argparse
import importlib.util
import json
from pathlib import Path
import re

spec = importlib.util.spec_from_file_location('batch', Path(__file__).with_name('review-google-batch.py'))
batch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(batch)
core, planner, digest = batch.core, batch.planner, batch.digest


def manifest(data):
    result = {}
    for line in data.decode('utf-8').splitlines():
        fields = line.split()
        if len(fields) != 2:
            raise ValueError('Malformed publisher manifest')
        path, value = fields
        if (not re.fullmatch('[A-Za-z0-9_+./-]+', path) or
                any(p in ('', '.', '..') for p in path.split('/')) or path in result or
                not re.fullmatch('[0-9a-f]{64}', value)):
            raise ValueError('Unsafe, duplicate or ambiguous manifest path')
        result[path] = value
    if not result:
        raise ValueError('Empty publisher manifest')
    return result


def review(evidence, metadata, prior_bytes, registry_bytes, control, request_pin, prior_pin, registry_pin):
    prior, rows, known = batch.checked_receipt(prior_bytes, metadata, prior_pin)
    if not re.fullmatch('[0-9a-f]{40}', control or '') or digest(registry_bytes) != registry_pin:
        raise ValueError('Uninspected control or release registry')
    registry = json.loads(registry_bytes)
    if registry.get('schema') != 1 or registry.get('publisher_repository') != 'JetBrains/kotlin':
        raise ValueError('Unsupported publisher registry')
    entries, versions = [], set()
    for source in registry['sources']:
        version = source['version']
        if not re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+', version) or version in versions:
            raise ValueError('Ambiguous source version')
        versions.add(version)
        url = 'https://github.com/JetBrains/kotlin/releases/download/v'+version+'/maven-'+version+'-sha256.txt'
        if (source['asset_url'] != url or source['release_url'] != 'https://github.com/JetBrains/kotlin/releases/tag/v'+version or
                not re.fullmatch('[0-9a-f]{64}', source['sha256']) or
                not 0 < source['size'] <= 262144 or not all(isinstance(source[k], int) and source[k] > 0 for k in ('release_id', 'asset_id'))):
            raise ValueError('Invalid official release asset identity')
        entries.append({'id': 'kotlin-manifest-'+version.replace('.', '-'), 'kind': 'publisher',
                        'url': url, 'sha256': source['sha256'], 'max_bytes': 262144})
    raw_request = core.bounded_bytes(evidence/'request.json', 65536)
    if digest(raw_request) != request_pin:
        raise ValueError('Captured request differs from the inspected source pin')
    request = planner.collector.read_policy(evidence/'request.json')
    raw_report = core.bounded_bytes(evidence/'report.json', 512*1024)
    report = json.loads(raw_report)
    if (request['entries'] != entries or report.get('control_commit') != control or
            report.get('scope') != 'PUBLISHER_EVIDENCE_ONLY' or report.get('automatically_authenticated') is not False or
            any(request[k] != prior[k] or report.get(k) != prior[k] for k in ('baseline_source_commit', 'baseline_metadata_sha256'))):
        raise ValueError('Publisher capture control, request or baseline mismatch')
    captures = report.get('results', [])
    if len(captures) != len(entries) or any(x.get('request') != y or x.get('authenticated') is not False for x, y in zip(captures, entries)):
        raise ValueError('Missing, reordered or automatically trusted captures')
    outcomes, approved = [], []
    for source, entry, capture in zip(registry['sources'], entries, captures):
        outcome = {'version': source['version'], 'asset_sha256': source['sha256'], 'status': capture.get('status'), 'verified_files': []}
        outcomes.append(outcome)
        if capture.get('status') != 'MATCHED_PIN':
            continue
        data = core.bounded_bytes(evidence/(entry['id']+'.bin'), entry['max_bytes'])
        if digest(data) != source['sha256'] or capture.get('sha256') != source['sha256'] or len(data) != source['size'] or capture.get('bytes') != len(data):
            raise ValueError('Publisher asset bytes differ from the inspected release')
        published = manifest(data)
        outcome['manifest_path_count'] = len(published)
        for identity, row in sorted(rows.items()):
            if identity in known or row['version'] != source['version'] or not (row['group'] == 'org.jetbrains.kotlin' or row['group'].startswith('org.jetbrains.kotlin.')):
                continue
            path = '/'.join((row['group'].replace('.', '/'), row['name'], row['version'], row['file']))
            if path not in published:
                continue
            if published[path] != row['sha256']:
                raise ValueError('Exact publisher artifact checksum conflicts with captured baseline')
            approved.append({**row, 'proof': 'Kotlin official v'+source['version']+' release checksum manifest SHA256 '+source['sha256']})
            outcome['verified_files'].append(list(identity))
            known.add(identity)
    prior['reviewed_artifacts'].extend(approved)
    prior['reviewed_artifact_count'] = len(known)
    prior['remaining_unreviewed_artifact_count'] = len(rows)-len(known)
    prior['complete_for_baseline'] = len(known) == len(rows)
    prior['publisher_manifest_batches'] = prior.get('publisher_manifest_batches', [])+[{
        'control_commit': control, 'request_sha256': request_pin, 'report_sha256': digest(raw_report),
        'prior_review_sha256': prior_pin, 'registry_sha256': registry_pin,
        'review_script_sha256': digest(Path(__file__).read_bytes()), 'verified_file_count': len(approved), 'outcomes': outcomes}]
    return prior, core.candidate_xml(prior['reviewed_artifacts'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    for arg in ('evidence', 'metadata', 'prior_review', 'registry', 'output'):
        parser.add_argument(arg, type=Path)
    for arg in ('control', 'request', 'prior', 'registry'):
        parser.add_argument('--expected-'+arg, required=True)
    args = parser.parse_args()
    record, xml = review(args.evidence, core.bounded_bytes(args.metadata, 3_000_000), core.bounded_bytes(args.prior_review, 2_000_000),
                         core.bounded_bytes(args.registry, 65536), args.expected_control, args.expected_request, args.expected_prior, args.expected_registry)
    args.output.mkdir(parents=True, exist_ok=False)
    (args.output/'dependency-provenance-review.json').write_text(json.dumps(record, indent=2)+'\n')
    (args.output/'reviewed-verification-candidate.xml').write_bytes(xml)
    print(json.dumps({'reviewed_total': record['reviewed_artifact_count'], 'remaining': record['remaining_unreviewed_artifact_count'],
                      'new_verified': record['publisher_manifest_batches'][-1]['verified_file_count'], 'active_policy_changed': False}))
