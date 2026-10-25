#!/usr/bin/env python3
"""Offline exact-file Okio review using the independently anchored historical issuer."""
import argparse
import hashlib
import importlib.util
import io
import json
from pathlib import Path
import zipfile

spec = importlib.util.spec_from_file_location('runtime', Path(__file__).with_name('review-runtime-publishers.py'))
runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runtime)
batch = runtime.batch
PRIMARY = 'DBD744ACE7ADE6AA50DD591F66B50994442D2D40'
PRIOR_SHA256 = '9dc7776b37f4fd9e8eee1c62e4dff3ce687ab507971489b72a7f01a3041c3e77'
ARCHIVE_SHA256 = '019f31f7526cbafc2b3d4da5e352bdb89334edd05954b24e9bafb655bcddfdc3'
REGISTRY_SHA256 = 'b3d34114ca6b80308aa74520aa6f4268ea5054dfa72adc1286abc6e5592ea9e8'
KEY_SHA256 = '4bf0a15d40e13fedb0736e5a6b1e331c94da9e36fb739316932256508bbd5255'
KEY_URL = 'https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x'+PRIMARY
EXACT = {
    'runtime-002': ('com.squareup.okio', 'okio', '3.4.0', 'okio-3.4.0.module'),
    'runtime-004': ('com.squareup.okio', 'okio-jvm', '3.4.0', 'okio-jvm-3.4.0.jar'),
    'runtime-005': ('com.squareup.okio', 'okio-jvm', '3.4.0', 'okio-jvm-3.4.0.module'),
}


def review(metadata, prior_bytes, registry_bytes, key, archive_bytes):
    prior, rows, known = batch.checked_receipt(prior_bytes, metadata, PRIOR_SHA256)
    for data, pin in ((registry_bytes, REGISTRY_SHA256), (key, KEY_SHA256), (archive_bytes, ARCHIVE_SHA256)):
        if batch.digest(data) != pin:
            raise ValueError('Pinned issuer, public key or historical capture changed')
    registry = json.loads(registry_bytes)
    publishers = [x for x in registry['publishers'] if x['id'] == 'square']
    if len(publishers) != 1:
        raise ValueError('Ambiguous publisher')
    anchor = publishers[0]
    source = anchor['source_text'].encode()
    blob = hashlib.sha1(b'blob '+str(len(source)).encode()+b'\0'+source).hexdigest()
    if (anchor['primary_fingerprint'] != PRIMARY or anchor['groups'] != ['com.squareup.okio'] or
            blob != anchor['source_blob'] or PRIMARY not in source.decode().replace(' ', '').upper() or
            batch.planner.collector.public_key_identity(key) != [PRIMARY]):
        raise ValueError('Independent issuer binding changed')
    approved, outcomes = [], []
    # Archive hash is fixed before parsing; never extract or execute any member.
    with zipfile.ZipFile(io.BytesIO(archive_bytes)) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)) or archive.testzip() is not None:
            raise ValueError('Duplicate or corrupt capture')
        captures = json.loads(archive.read('report.json'))['results']
        for entry, identity in EXACT.items():
            if identity not in rows or identity in known:
                raise ValueError('Exact frozen identity absent or already reviewed')
            matches = [x for x in captures if x['request']['id'] == entry]
            if len(matches) != 1:
                raise ValueError('Missing or ambiguous exact capture')
            capture, row = matches[0], rows[identity]
            request = capture['request']
            expected_url = 'https://repo.maven.apache.org/maven2/'+'/'.join((identity[0].replace('.', '/'), *identity[1:]))
            data, signature = archive.read(entry+'.bin'), archive.read(entry+'.asc')
            if (request['coordinate'] != ':'.join(identity[:3]) or request['file'] != identity[3] or
                    request['url'] != expected_url or capture['status'] != 'MATCHED_PIN' or
                    capture['authenticated'] is not False or len(data) != capture['bytes'] or
                    any(x != row['sha256'] for x in (batch.digest(data), capture['sha256'], request['sha256'])) or
                    capture['signature']['status'] != 'CAPTURED' or
                    batch.digest(signature) != capture['signature']['sha256']):
                raise ValueError('Exact artifact or detached signature capture differs')
            status = runtime.verify(key, signature, data, PRIMARY)
            approved.append({**row, 'proof_kind':'DETACHED_PGP_SIGNATURE',
                'proof':'Square Okio exact detached PGP; primary '+PRIMARY+'; independently published issuer '+
                        anchor['source_repository']+'@'+anchor['source_commit']+'/'+anchor['source_path']+
                        '; public key SHA256 '+KEY_SHA256+'; signer '+status.split()[2]})
            outcomes.append({'identity':list(identity), 'sha256':row['sha256'], 'bytes':len(data),
                             'signature_sha256':batch.digest(signature), 'validsig_status':status})
    prior['reviewed_artifacts'].extend(approved)
    count = len(prior['reviewed_artifacts'])
    prior.update(reviewed_artifact_count=count, remaining_unreviewed_artifact_count=len(rows)-count,
                 complete_for_baseline=count == len(rows))
    prior['okio_publisher_reviews'] = [{
        'prior_review_sha256':PRIOR_SHA256, 'registry_sha256':REGISTRY_SHA256,
        'archive_sha256':ARCHIVE_SHA256, 'public_key_sha256':KEY_SHA256, 'public_key_url':KEY_URL,
        'primary_fingerprint':PRIMARY, 'source_commit':anchor['source_commit'], 'source_blob':anchor['source_blob'],
        'reviewer_sha256':batch.digest(Path(__file__).read_bytes()),
        'signature_verifier_sha256':batch.digest(Path(runtime.__file__).read_bytes()),
        'verified_new_files':len(approved), 'outcomes':outcomes,
        'namespace_wide_trust':False, 'active_gradle_policy_changed':False,
        'key_freshness_or_absent_revocations_claim':False}]
    return prior, batch.core.candidate_xml(prior['reviewed_artifacts'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('metadata', 'prior', 'registry', 'key', 'archive', 'output'):
        parser.add_argument(name, type=Path)
    args = parser.parse_args()
    record, xml = review(*(runtime.core.bounded_bytes(getattr(args, name), limit) for name, limit in
                          (('metadata',3_000_000), ('prior',2_000_000), ('registry',65_536),
                           ('key',131_072), ('archive',3_000_000))))
    args.output.mkdir(parents=True, exist_ok=False)
    (args.output/'dependency-provenance-review.json').write_text(json.dumps(record, indent=2)+'\n')
    (args.output/'reviewed-verification-candidate.xml').write_bytes(xml)
    print(json.dumps({'reviewed':record['reviewed_artifact_count'],
                      'pending':record['remaining_unreviewed_artifact_count'],
                      'new_verified':len(EXACT), 'active_policy_changed':False}))
