#!/usr/bin/env python3
"""Replay exact Google module copies and two explicitly scoped Guava files, offline."""
import argparse
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import zipfile

spec = importlib.util.spec_from_file_location('copies', Path(__file__).with_name('review-runtime-source-copies.py'))
copies = importlib.util.module_from_spec(spec)
spec.loader.exec_module(copies)
batch = copies.batch
PRIOR = '4b2c787862a85ca38ca2dbf24409d49964a42f1db78648ad61d67d3270bf34d7'
REPORT = '92fd7224a75743ea3dfbb5202dc551cb9855c7162e2e04a80f80335b72945ed7'
OWNERSHIP = '3dfeb0d43e0627bf01adc97f7273829127fc252138d7436e788d6c636de9700a'
DIRECTORY = 'fd31bf6f9310d84bc94beb30e6880d9945587d94bea324f88b587474cdc46d22'
ARCHIVE = '019f31f7526cbafc2b3d4da5e352bdb89334edd05954b24e9bafb655bcddfdc3'
COLLECTOR = '32444850b73e8ac97113d24c5338fd06409e4475855286aa9da7d19b1a0e8889'
GUAVA_COMMIT = '5ae56d77e64305752b919ff2c96713f47ac0c94c'


def blob(data):
    return hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()


def read(root, filename, limit, pin=None):
    data = batch.core.bounded_bytes(root/filename, limit)
    if pin and batch.digest(data) != pin:
        raise ValueError('Pinned evidence changed: '+filename)
    return data


def module_plan(rows, known):
    return [(i, row) for i, row in sorted(rows.items()) if i not in known and
            row['group'].startswith(('androidx.', 'com.android.')) and
            row['file'] == row['name']+'-'+row['version']+'.module']


def review(metadata, prior_bytes, evidence, archive_bytes):
    prior, rows, known = batch.checked_receipt(prior_bytes, metadata, PRIOR)
    report = json.loads(read(evidence, 'report.json', 524288, REPORT))
    # The original imported collector's runtime scope label is retained verbatim.
    # Its recorded selection was adapted to all pending Google modules; the
    # independently recomputed exact plan below establishes this review's scope.
    selected = module_plan(rows, known)
    if (len(selected) != 47 or report.get('schema') != 1 or
            report.get('scope') != copies.SCOPE or report.get('automatic_authentication') is not False or
            report.get('repository') != copies.source.REPOSITORY or report.get('commit') != copies.source.COMMIT or
            report.get('tag_object') != copies.source.TAG_OBJECT or report.get('prior_review_sha256') != PRIOR or
            report.get('metadata_sha256') != batch.digest(metadata) or report.get('collector_sha256') != COLLECTOR or
            [tuple(x['identity']) for x in report['results']] != [i for i, _ in selected]):
        raise ValueError('Capture release, producer, exact selection or approval flag differs')
    approved, outcomes = [], []
    for index, (identity, row) in enumerate(selected):
        item = report['results'][index]
        label = 'runtime-'+str(index).zfill(3)
        if item['id'] != label or item['url'] != copies.route(identity) or item['expected_sha256'] != row['sha256']:
            raise ValueError('Unexpected exact source route or identity')
        outcome = {'identity':list(identity), 'status':item['status']}
        outcomes.append(outcome)
        if item['status'] == 'DIRECTORY_HTTP_404':
            continue
        if item['status'] != 'MATCHED_FROZEN_BYTES':
            raise ValueError('Unexpected capture status')
        directory = read(evidence, label+'.directory.json', 65536)
        data = read(evidence, label+'.module', 4*1024*1024)
        data.decode('utf-8')
        proof = copies.checked_artifact(identity, row, directory, data)
        if any(item.get(k) != v for k, v in proof.items()):
            raise ValueError('Actual source bytes differ from the capture receipt')
        approved.append({**row, 'proof_kind':'PUBLISHER_MAINTAINED_SOURCE_COPY_NOT_PGP',
                         'proof':'Google publisher-maintained AOSP release copy '+copies.route(identity)+
                                 '; Git blob '+proof['blob_sha1']+'; exact SHA256 '+row['sha256']})
        outcome.update(proof)
    ownership = json.loads(read(evidence, 'guava-ownership.json', 32768, OWNERSHIP))
    if (ownership['repository'] != 'google/guava' or ownership['commit'] != GUAVA_COMMIT or
            ownership['readme_blob'] != 'b17c27c4af8c827082bee4b5be25fa4354b3b9e6' or
            ownership['pom_path'] != 'futures/listenablefuture1/pom.xml' or
            ownership['pom_blob'] != '7277316863b61d1039e5cf4760317db62715171f' or
            blob(ownership['readme'].encode()) != ownership['readme_blob'] or
            blob(ownership['pom'].encode()) != ownership['pom_blob']):
        raise ValueError('Independent exact Guava ownership source changed')
    if batch.digest(archive_bytes) != ARCHIVE:
        raise ValueError('Existing exact artifact capture changed')
    directory = read(evidence, 'guava.directory.json', 65536, DIRECTORY)
    with zipfile.ZipFile(io.BytesIO(archive_bytes)) as archive:
        if len(archive.namelist()) != len(set(archive.namelist())) or archive.testzip() is not None:
            raise ValueError('Ambiguous or corrupt existing capture')
        captures = json.loads(archive.read('report.json'))['results']
        for entry, ext in [('runtime-000', 'jar'), ('runtime-001', 'pom')]:
            identity = ('com.google.guava', 'listenablefuture', '1.0', 'listenablefuture-1.0.'+ext)
            if identity not in rows or identity in known:
                raise ValueError('Exact Guava frozen identity missing or already reviewed')
            row, data = rows[identity], archive.read(entry+'.bin')
            matches = [x for x in captures if x['request']['id'] == entry]
            if (len(matches) != 1 or matches[0]['request']['coordinate'] != ':'.join(identity[:3]) or
                    matches[0]['request']['file'] != identity[-1] or matches[0]['sha256'] != row['sha256'] or
                    matches[0]['status'] != 'MATCHED_PIN' or matches[0]['bytes'] != len(data)):
                raise ValueError('Guava artifact capture identity changed')
            proof = copies.checked_artifact(identity, row, directory, data)
            approved.append({**row, 'proof_kind':'PUBLISHER_MAINTAINED_SOURCE_COPY_NOT_PGP',
                'proof':'Google Guava ownership google/guava@'+GUAVA_COMMIT+'/'+ownership['pom_path']+
                        '; exact Google-maintained AOSP copy '+copies.route(identity)+
                        '; Git blob '+proof['blob_sha1']+'; exact SHA256 '+row['sha256']})
            outcomes.append({'identity':list(identity), 'status':'MATCHED_FROZEN_BYTES', **proof})
    prior['reviewed_artifacts'].extend(approved)
    count = len(prior['reviewed_artifacts'])
    prior.update(reviewed_artifact_count=count, remaining_unreviewed_artifact_count=len(rows)-count,
                 complete_for_baseline=count == len(rows))
    prior['google_source_extension_reviews'] = [{
        'scope':'EXACT_GOOGLE_MODULES_AND_TWO_EXPLICIT_GUAVA_FILES_NOT_PGP',
        'prior_review_sha256':PRIOR, 'capture_report_sha256':REPORT,
        'guava_ownership_sha256':OWNERSHIP, 'guava_directory_sha256':DIRECTORY,
        'existing_artifact_archive_sha256':ARCHIVE, 'guava_source_commit':GUAVA_COMMIT,
        'repository':copies.source.REPOSITORY, 'commit':copies.source.COMMIT,
        'tag_object':copies.source.TAG_OBJECT, 'reviewer_sha256':batch.digest(Path(__file__).read_bytes()),
        'source_checker_sha256':batch.digest(Path(copies.__file__).read_bytes()),
        'verified_new_files':len(approved), 'outcomes':outcomes, 'pgp_signature_claim':False,
        'namespace_wide_trust':False, 'active_gradle_policy_changed':False}]
    return prior, batch.core.candidate_xml(prior['reviewed_artifacts'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for arg in ('metadata', 'prior', 'evidence', 'archive', 'output'):
        parser.add_argument(arg, type=Path)
    args = parser.parse_args()
    record, xml = review(batch.core.bounded_bytes(args.metadata,3_000_000),
                         batch.core.bounded_bytes(args.prior,2_000_000), args.evidence,
                         batch.core.bounded_bytes(args.archive,3_000_000))
    args.output.mkdir(parents=True,exist_ok=False)
    (args.output/'dependency-provenance-review.json').write_text(json.dumps(record,indent=2)+'\n')
    (args.output/'reviewed-verification-candidate.xml').write_bytes(xml)
    print(json.dumps({'reviewed':record['reviewed_artifact_count'],
                      'pending':record['remaining_unreviewed_artifact_count'],
                      'new_verified':record['google_source_extension_reviews'][0]['verified_new_files'],
                      'active_policy_changed':False}))
