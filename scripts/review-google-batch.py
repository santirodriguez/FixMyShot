#!/usr/bin/env python3
"""Extend an explicitly pinned reviewed receipt with independently verified exact files."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re

spec = importlib.util.spec_from_file_location('planner', Path(__file__).with_name('plan-google-batch.py'))
planner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(planner)
core = planner.reviewer


def digest(data):
    return hashlib.sha256(data).hexdigest()


def checked_receipt(data, metadata, expected_digest):
    if re.fullmatch('[0-9a-f]{64}', expected_digest or '') is None or digest(data) != expected_digest:
        raise ValueError('Prior reviewed receipt differs from the inspected source pin')
    record = json.loads(data)
    rows = {planner.identity(x): x for x in core.captured_rows(metadata)}
    if (record.get('status') != 'PARTIAL_PUBLISHER_REVIEW__NOT_ACTIVE' or
            record.get('active_gradle_policy_changed') is not False or
            record.get('baseline_metadata_sha256') != digest(metadata)):
        raise ValueError('Prior review baseline or active-policy mismatch')
    seen = set()
    for row in record['reviewed_artifacts']:
        identity = planner.identity(row)
        if identity in seen or identity not in rows or row['sha256'] != rows[identity]['sha256'] or not row.get('proof'):
            raise ValueError('Prior receipt has duplicate, unknown or conflicting artifacts')
        seen.add(identity)
    if (record['reviewed_artifact_count'] != len(seen) or record['captured_artifact_count'] != len(rows) or
            record['remaining_unreviewed_artifact_count'] != len(rows)-len(seen)):
        raise ValueError('Prior reviewed receipt count mismatch')
    return record, rows, seen


def review(evidence, metadata, prior_bytes, key, expected_control, expected_request, expected_prior):
    prior, rows, known = checked_receipt(prior_bytes, metadata, expected_prior)
    if re.fullmatch('[0-9a-f]{40}', expected_control or '') is None or digest(key) != core.GOOGLE_KEYS_SHA256:
        raise ValueError('Uninspected batch control or publisher key')
    raw_request = core.bounded_bytes(evidence/'request.json', 65536)
    if re.fullmatch('[0-9a-f]{64}', expected_request or '') is None or digest(raw_request) != expected_request:
        raise ValueError('Batch request differs from the inspected source pin')
    request = planner.collector.read_policy(evidence/'request.json')
    raw_report = core.bounded_bytes(evidence/'report.json', 512*1024)
    report = json.loads(raw_report)
    if (report.get('control_commit') != expected_control or report.get('scope') != 'PUBLISHER_EVIDENCE_ONLY' or
            report.get('automatically_authenticated') is not False or
            any(request[k] != prior[k] or report.get(k) != prior[k]
                for k in ('baseline_source_commit', 'baseline_metadata_sha256'))):
        raise ValueError('Batch capture source/control mismatch')
    captures = report.get('results', [])
    if len(captures) != len(request['entries']) or any(x.get('request') != y for x, y in zip(captures, request['entries'])):
        raise ValueError('Incomplete or reordered capture identities')
    outcomes, approved = [], []
    for item, capture in zip(request['entries'], captures):
        if item['kind'] != 'signed-artifact':
            raise ValueError('Unsupported batch evidence type')
        coordinate = item['coordinate'].split(':')
        if len(coordinate) != 3:
            raise ValueError('Ambiguous batch coordinate')
        identity = tuple(coordinate+[item['file']])
        row = rows.get(identity)
        if row is None or row['sha256'] != item['sha256']:
            raise ValueError('Batch artifact is not the exact baseline identity')
        remote = planner.remote_filename(row)
        url = 'https://dl.google.com/dl/android/maven2/'+row['group'].replace('.', '/')+'/'+row['name']+'/'+row['version']+'/'+remote
        if item['url'] != url or capture.get('authenticated') is not False:
            raise ValueError('Batch origin or automatic-authentication mismatch')
        outcome = {'identity': list(identity), 'sha256': row['sha256'], 'capture_status': capture.get('status')}
        outcomes.append(outcome)
        if identity in known:
            outcome['status'] = 'ALREADY_REVIEWED'
            continue
        if capture.get('status') != 'MATCHED_PIN':
            outcome['status'] = 'CAPTURE_PIN_CONFLICT' if capture.get('status') == 'SHA256_MISMATCH' else 'CAPTURE_UNAVAILABLE'
            continue
        data = core.bounded_bytes(evidence/(item['id']+'.bin'), item['max_bytes'])
        if digest(data) != item['sha256'] or capture.get('sha256') != item['sha256'] or capture.get('bytes') != len(data):
            raise ValueError('Captured artifact bytes conflict with the source pin')
        signature_record = capture.get('signature', {})
        if signature_record.get('status') != 'CAPTURED':
            outcome.update(status='SIGNATURE_ABSENT', signature_capture_status=signature_record.get('status', 'UNAVAILABLE'))
            continue
        signature = core.bounded_bytes(evidence/(item['id']+'.asc'), 131072)
        if digest(signature) != signature_record.get('sha256'):
            raise ValueError('Captured detached signature bytes changed')
        try:
            status = core.verify_google(key, signature, data)
        except ValueError:
            outcome.update(status='SIGNATURE_UNVERIFIED', signature_sha256=digest(signature))
            continue
        outcome.update(status='VERIFIED', signature_sha256=digest(signature), validsig_status=status)
        approved.append({**row, 'proof': 'Google detached PGP; primary '+core.GOOGLE_PRIMARY+'; signer '+status.split()[2]})
        known.add(identity)
    prior['reviewed_artifacts'].extend(approved)
    prior['reviewed_artifact_count'] = len(known)
    prior['remaining_unreviewed_artifact_count'] = len(rows)-len(known)
    prior['complete_for_baseline'] = len(rows) == len(known)
    prior['batch_review_script_sha256'] = digest(Path(__file__).read_bytes())
    prior['batch_verifier_script_sha256'] = digest(Path(core.__file__).read_bytes())
    prior['publisher_batches'] = prior.get('publisher_batches', []) + [{
        'control_commit': expected_control, 'request_sha256': expected_request, 'report_sha256': digest(raw_report),
        'prior_review_sha256': expected_prior, 'publisher_key_sha256': digest(key),
        'verified_file_count': len(approved), 'outcomes': outcomes}]
    return prior, core.candidate_xml(prior['reviewed_artifacts'])


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('evidence', type=Path)
    parser.add_argument('metadata', type=Path)
    parser.add_argument('prior_review', type=Path)
    parser.add_argument('publisher_key', type=Path)
    parser.add_argument('output', type=Path)
    parser.add_argument('--expected-control', required=True)
    parser.add_argument('--expected-request-sha256', required=True)
    parser.add_argument('--expected-prior-sha256', required=True)
    args = parser.parse_args()
    result, xml = review(args.evidence, core.bounded_bytes(args.metadata, 3_000_000),
                         core.bounded_bytes(args.prior_review, 2_000_000), core.bounded_bytes(args.publisher_key, 131072),
                         args.expected_control, args.expected_request_sha256, args.expected_prior_sha256)
    args.output.mkdir(parents=True, exist_ok=False)
    (args.output/'dependency-provenance-review.json').write_text(json.dumps(result, indent=2)+'\n')
    (args.output/'reviewed-verification-candidate.xml').write_bytes(xml)
    batch = result['publisher_batches'][-1]
    print(json.dumps({'reviewed_total': result['reviewed_artifact_count'],
                      'remaining': result['remaining_unreviewed_artifact_count'],
                      'new_verified': batch['verified_file_count'],
                      'attempted': len(batch['outcomes']), 'active_policy_changed': False}))


if __name__ == '__main__':
    main()
