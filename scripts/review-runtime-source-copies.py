#!/usr/bin/env python3
"""Capture/replay exact Google runtime artifacts from one independently pinned AOSP release."""
import argparse
import base64
from concurrent.futures import ThreadPoolExecutor
import hashlib
import importlib.util
import json
from pathlib import Path
import threading
import time
import urllib.error
import urllib.request

spec = importlib.util.spec_from_file_location('source', Path(__file__).with_name('review-google-source-copy.py'))
source = importlib.util.module_from_spec(spec)
spec.loader.exec_module(source)
batch = source.batch
SCOPE = 'EXACT_GOOGLE_APP_RUNTIME_SOURCE_COPIES_ONE_PINNED_AOSP_RELEASE'
MAX_FILE = 4 * 1024 * 1024


def inputs(metadata, prior_bytes, lock, prior_pin, lock_pin):
    prior, rows, known = batch.checked_receipt(prior_bytes, metadata, prior_pin)
    if batch.digest(lock) != lock_pin:
        raise ValueError('Uninspected runtime lock')
    runtime = {line.split('=', 1)[0] for line in lock.decode().splitlines()
               if '=' in line and 'releaseRuntimeClasspath' in line.split('=', 1)[1].split(',')}
    if not runtime:
        raise ValueError('Missing release runtime selection')
    attempted = {tuple(x['identity']) for old in prior.get('runtime_source_copy_batches', [])
                 if old.get('commit') == source.COMMIT for x in old['outcomes']}
    selected = []
    for identity, row in sorted(rows.items()):
        if (identity in known or identity in attempted or ':'.join(identity[:3]) not in runtime or
                not row['group'].startswith(('androidx.', 'com.android.'))):
            continue
        if row['file'] not in [row['name']+'-'+row['version']+'.'+ext for ext in ('aar', 'jar', 'module')]:
            continue
        batch.planner.remote_filename(row)
        selected.append((identity, row))
    if not selected or len(selected) > 96:
        raise ValueError('Empty or excessive unattempted source plan')
    return prior, rows, selected


def route(identity):
    group, name, version, filename = identity
    return source.REPOSITORY+'/+/'+source.COMMIT+'/common/m2/repository/'+group.replace('.', '/')+'/'+name+'/'+version+'/'+filename


def checked_artifact(identity, row, directory_bytes, data):
    if len(directory_bytes) > 65536 or not directory_bytes.startswith(b")]}'\n") or len(data) > MAX_FILE:
        raise ValueError('Oversized source or unexpected Gitiles framing')
    directory = json.loads(directory_bytes.split(b'\n', 1)[1])
    entries = [x for x in directory['entries'] if x.get('name') == identity[-1]]
    blob = hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
    if (len(entries) != 1 or entries[0].get('type') != 'blob' or entries[0].get('mode') != 0o100644 or
            entries[0].get('id') != blob or batch.digest(data) != row['sha256']):
        raise ValueError('Exact source bytes, regular blob membership or frozen checksum differs')
    return {'bytes':len(data), 'sha256':batch.digest(data), 'blob_sha1':blob,
            'directory_tree':directory['id'], 'directory_sha256':batch.digest(directory_bytes)}


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        raise ValueError('Publisher source redirect refused')


def collect(metadata, prior_bytes, lock, prior_pin, lock_pin, output):
    _, _, selected = inputs(metadata, prior_bytes, lock, prior_pin, lock_pin)
    output.mkdir(parents=True, exist_ok=False)
    deadline = time.monotonic()+120
    remaining = [32*1024*1024]
    guard = threading.Lock()

    def get(url, limit):
        left = deadline-time.monotonic()
        if left <= 0:
            raise TimeoutError('Source lookup time budget exhausted')
        with urllib.request.build_opener(NoRedirect()).open(url, timeout=min(15, left)) as response:
            if response.geturl() != url or response.status != 200:
                raise ValueError('Publisher source origin/status changed')
            data = response.read(limit+1)
        with guard:
            remaining[0] -= len(data)
            if len(data) > limit or remaining[0] < 0 or time.monotonic() > deadline:
                raise ValueError('Source lookup byte/time budget exhausted')
        return data

    # One directory lookup per coordinate, shared by its exact pending files.
    grouped = {}
    for index, (identity, row) in enumerate(selected):
        grouped.setdefault(identity[:3], []).append((index, identity, row))

    def one(group):
        results = []
        directory = None
        directory_error = None
        for index, identity, row in group:
            label = 'runtime-'+str(index).zfill(3)
            item = {'id':label, 'identity':list(identity), 'expected_sha256':row['sha256'],
                    'url':route(identity), 'status':'UNAVAILABLE'}
            stage = 'DIRECTORY'
            try:
                if directory_error:
                    raise ValueError(directory_error)
                if directory is None:
                    try:
                        directory = get(route(identity).rsplit('/', 1)[0]+'/?format=JSON', 65536)
                    except (OSError, ValueError) as error:
                        directory_error = type(error).__name__
                        raise
                parsed = json.loads(directory.split(b'\n', 1)[1])
                if not any(x.get('name') == identity[-1] for x in parsed['entries']):
                    item['status'] = 'FILE_ABSENT_IN_EXACT_SOURCE_DIRECTORY'
                else:
                    stage = 'ARTIFACT'
                    encoded = get(route(identity)+'?format=TEXT', (MAX_FILE+2)//3*4+8)
                    data = base64.b64decode(encoded, validate=True)
                    proof = checked_artifact(identity, row, directory, data)
                    (output/(label+'.bin')).write_bytes(data)
                    item.update(proof, status='MATCHED_FROZEN_BYTES')
                (output/(label+'.directory.json')).write_bytes(directory)
                item['directory_sha256'] = batch.digest(directory)
            except urllib.error.HTTPError as error:
                item['status'] = stage+'_HTTP_'+str(error.code)
            except (OSError, ValueError, KeyError, IndexError) as error:
                item['status'] = stage+'_UNAVAILABLE_'+type(error).__name__
            results.append(item)
        return results

    with ThreadPoolExecutor(max_workers=6) as pool:
        results = [item for group in pool.map(one, grouped.values()) for item in group]
    results.sort(key=lambda x:x['id'])
    report = {'schema':1, 'scope':SCOPE, 'repository':source.REPOSITORY, 'commit':source.COMMIT,
              'tag_object':source.TAG_OBJECT, 'metadata_sha256':batch.digest(metadata),
              'prior_review_sha256':prior_pin, 'app_lock_sha256':lock_pin,
              'collector_sha256':batch.digest(Path(__file__).read_bytes()),
              'automatic_authentication':False, 'results':results}
    (output/'report.json').write_text(json.dumps(report, indent=2)+'\n')
    return report


def review(metadata, prior_bytes, lock, prior_pin, lock_pin, report_bytes, report_pin, evidence):
    prior, rows, selected = inputs(metadata, prior_bytes, lock, prior_pin, lock_pin)
    if batch.digest(report_bytes) != report_pin:
        raise ValueError('Source capture report pin changed')
    report = json.loads(report_bytes)
    expected = {'schema':1, 'scope':SCOPE, 'repository':source.REPOSITORY, 'commit':source.COMMIT,
                'tag_object':source.TAG_OBJECT, 'metadata_sha256':batch.digest(metadata),
                'prior_review_sha256':prior_pin, 'app_lock_sha256':lock_pin,
                'collector_sha256':batch.digest(Path(__file__).read_bytes()), 'automatic_authentication':False}
    if any(report.get(k) != v for k,v in expected.items()) or [tuple(x['identity']) for x in report['results']] != [x[0] for x in selected]:
        raise ValueError('Source release, producer, runtime scope, pins, order or approval flag changed')
    approved = []
    outcomes = []
    for index, item in enumerate(report['results']):
        identity, row = selected[index]
        label = 'runtime-'+str(index).zfill(3)
        if item['id'] != label or item['url'] != route(identity) or item['expected_sha256'] != row['sha256']:
            raise ValueError('Source capture exact identity or route changed')
        outcomes.append({'identity':list(identity), 'status':item['status']})
        if item['status'] != 'MATCHED_FROZEN_BYTES':
            continue
        directory = batch.core.bounded_bytes(evidence/(label+'.directory.json'), 65536)
        data = batch.core.bounded_bytes(evidence/(label+'.bin'), MAX_FILE)
        proof = checked_artifact(identity, row, directory, data)
        if any(item.get(k) != v for k,v in proof.items()):
            raise ValueError('Source capture receipt differs from actual bytes')
        approved.append({**row, 'proof_kind':'PUBLISHER_MAINTAINED_SOURCE_COPY_NOT_PGP',
                         'proof':'Google publisher-maintained AOSP release copy '+route(identity)+'; Git blob '+proof['blob_sha1']+'; exact SHA256 '+row['sha256']})
    prior['reviewed_artifacts'].extend(approved)
    count = len(prior['reviewed_artifacts'])
    prior.update(reviewed_artifact_count=count, remaining_unreviewed_artifact_count=len(rows)-count,
                 complete_for_baseline=count == len(rows))
    prior['runtime_source_copy_batches'] = prior.get('runtime_source_copy_batches', [])+[{
        **expected, 'capture_report_sha256':report_pin, 'verified_new_files':len(approved),
        'outcomes':outcomes, 'pgp_signature_claim':False, 'namespace_wide_trust':False,
        'active_gradle_policy_changed':False}]
    return prior, batch.core.candidate_xml(prior['reviewed_artifacts'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=('collect', 'review'))
    for arg in ('metadata', 'prior', 'lock', 'evidence'):
        parser.add_argument(arg, type=Path)
    parser.add_argument('--expected-prior', required=True)
    parser.add_argument('--expected-lock', required=True)
    parser.add_argument('--expected-report')
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    metadata = batch.core.bounded_bytes(args.metadata, 3_000_000)
    prior = batch.core.bounded_bytes(args.prior, 2_000_000)
    lock = batch.core.bounded_bytes(args.lock, 262144)
    if args.action == 'collect':
        report = collect(metadata, prior, lock, args.expected_prior, args.expected_lock, args.evidence)
        print(json.dumps({'rows':len(report['results']), 'matched':sum(x['status']=='MATCHED_FROZEN_BYTES' for x in report['results']), 'automatic_authentication':False}))
    else:
        if not args.expected_report or not args.output:
            parser.error('review requires --expected-report and --output')
        report = batch.core.bounded_bytes(args.evidence/'report.json', 524288)
        record, xml = review(metadata, prior, lock, args.expected_prior, args.expected_lock, report, args.expected_report, args.evidence)
        args.output.mkdir(parents=True, exist_ok=False)
        (args.output/'dependency-provenance-review.json').write_text(json.dumps(record, indent=2)+'\n')
        (args.output/'reviewed-verification-candidate.xml').write_bytes(xml)
        print(json.dumps({'reviewed':record['reviewed_artifact_count'], 'pending':record['remaining_unreviewed_artifact_count'], 'new_verified':record['runtime_source_copy_batches'][-1]['verified_new_files'], 'active_policy_changed':False}))
