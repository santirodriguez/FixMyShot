#!/usr/bin/env python3
"""Plan bounded Google Maven retrieval; namespace routing is never issuer approval."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re


def helper(name):
    spec = importlib.util.spec_from_file_location(name, Path(__file__).with_name(name+'.py'))
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


reviewer = helper('review-dependency-provenance')
collector = helper('collect-dependency-provenance')


def identity(row):
    return tuple(row[k] for k in ('group', 'name', 'version', 'file'))


def remote_filename(row):
    for key in ('group', 'name', 'version', 'file'):
        if re.fullmatch('[A-Za-z0-9_+.-]{1,200}', row[key]) is None or row[key] in ('.', '..'):
            raise ValueError('Unsafe Maven identity')
    extension = row['file'].rsplit('.', 1)[-1]
    if extension not in ('aar', 'jar', 'pom', 'module'):
        raise ValueError('Unsupported batch file type')
    if row['name']+'-'+row['version'] in row['file']:
        return row['file']
    # Gradle module aliases (ui.aar, annotation-metadata-*.jar) retain the
    # captured file name while requesting the canonical Maven publication.
    return row['name']+'-'+row['version']+'.'+extension


def plan(metadata, record):
    digest = hashlib.sha256(metadata).hexdigest()
    if record['baseline_metadata_sha256'] != digest or record.get('active_gradle_policy_changed') is not False:
        raise ValueError('Baseline or prior partial-review identity mismatch')
    known = {identity(x) for x in record['reviewed_artifacts']}
    attempted = {tuple(x['identity']) for batch in record.get('publisher_batches', []) for x in batch['outcomes']}
    rows = sorted((x for x in reviewer.captured_rows(metadata) if identity(x) not in known | attempted and
                   (x['group'].startswith('androidx.') or x['group'].startswith('com.android.'))), key=identity)
    binaries = [x for x in rows if x['file'].endswith(('.jar', '.aar'))][:48]
    modules = [x for x in rows if x['file'].endswith('.module')][:24]
    poms = [x for x in rows if x['file'].endswith('.pom')][:24]
    entries = []
    for index, row in enumerate(binaries+modules+poms):
        remote = remote_filename(row)
        limit = 512*1024 if row['file'].endswith(('.pom', '.module')) else 4*1024*1024
        if row['name'] in ('foundation-android', 'ui-android', 'ui-text-android', 'material3-android') and row['file'].endswith('.aar'):
            limit = 8*1024*1024
        entries.append({'id': 'batch-'+str(index).zfill(3), 'kind': 'signed-artifact',
                        'coordinate': ':'.join(row[k] for k in ('group', 'name', 'version')),
                        'file': row['file'], 'url': 'https://dl.google.com/dl/android/maven2/'+
                        row['group'].replace('.', '/')+'/'+row['name']+'/'+row['version']+'/'+remote,
                        'sha256': row['sha256'], 'max_bytes': limit})
    return {'schema': 1, 'baseline_source_commit': record['baseline_source_commit'],
            'baseline_metadata_sha256': digest, 'entries': entries}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('metadata', type=Path)
    parser.add_argument('prior_review', type=Path)
    parser.add_argument('output', type=Path)
    args = parser.parse_args()
    policy = plan(args.metadata.read_bytes(), json.loads(args.prior_review.read_text()))
    # Creation is explicit; no network, build, trust change or retry occurs here.
    with args.output.open('x') as stream:
        stream.write(json.dumps(policy, indent=2)+'\n')
    collector.read_policy(args.output)
    print(json.dumps({'entries': len(policy['entries']), 'declared_bytes_including_signatures':
                      sum(x['max_bytes']+131072 for x in policy['entries']),
                      'automatic_authentication': False}))


if __name__ == '__main__':
    main()
