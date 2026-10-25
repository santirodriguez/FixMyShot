#!/usr/bin/env python3
"""Review one exact Google Compose BOM POM against a pinned publisher source copy."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re

spec = importlib.util.spec_from_file_location('batch', Path(__file__).with_name('review-google-batch.py'))
batch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(batch)
IDENTITY = ('androidx.compose', 'compose-bom', '2025.12.00', 'compose-bom-2025.12.00.pom')
REPOSITORY = 'https://android.googlesource.com/platform/prebuilts/tools'
COMMIT = '7fd4bf2b791a54211e000dba6b24d696773dc792'
TAG_OBJECT = 'eeb949f3e8d3a0305fbf4bd01b3b3bfb9d223b27'
SOURCE_PATH = 'common/m2/repository/androidx/compose/compose-bom/2025.12.00/compose-bom-2025.12.00.pom'


def review(metadata, prior_bytes, registry_bytes, directory_bytes, data, prior_pin, registry_pin, *, allowed_identity=IDENTITY):
    prior, rows, known = batch.checked_receipt(prior_bytes, metadata, prior_pin)
    identity = tuple(allowed_identity)
    if (len(identity) != 4 or not identity[0].startswith(('androidx.', 'com.android.')) or
            any(re.fullmatch('[A-Za-z0-9_+.-]{1,200}', x) is None or x in ('.', '..') for x in identity) or
            identity[3] != identity[1]+'-'+identity[2]+'.pom'):
        raise ValueError('Source-copy scope is limited to exact Google-published POMs')
    source_path = 'common/m2/repository/'+identity[0].replace('.', '/')+'/'+identity[1]+'/'+identity[2]+'/'+identity[3]
    if batch.digest(registry_bytes) != registry_pin:
        raise ValueError('Publisher source registry differs from its inspected pin')
    source = json.loads(registry_bytes)
    if (source.get('schema') != 1 or tuple(source.get('identity', [])) != identity or
            source.get('repository') != REPOSITORY or source.get('commit') != COMMIT or
            source.get('tag') != 'studio-2026.2.1' or source.get('tag_object') != TAG_OBJECT or
            source.get('path') != source_path or
            source.get('url') != REPOSITORY+'/+/'+COMMIT+'/'+source_path):
        raise ValueError('Unreviewed publisher, release anchor or exact file scope')
    if batch.digest(directory_bytes) != source['directory_sha256']:
        raise ValueError('Publisher directory bytes changed')
    if not directory_bytes.startswith(b")]}'\n"):
        raise ValueError('Unexpected Gitiles directory framing')
    directory = json.loads(directory_bytes.split(b'\n', 1)[1])
    if directory.get('id') != source['directory_tree']:
        raise ValueError('Publisher directory tree changed')
    entries = [x for x in directory['entries'] if x.get('name') == identity[-1]]
    blob = hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
    row = rows.get(identity)
    if (len(entries) != 1 or entries[0].get('type') != 'blob' or entries[0].get('mode') != 0o100644 or
            entries[0].get('id') != blob or blob != source['blob_sha1'] or
            row is None or len(data) != source['bytes'] or
            batch.digest(data) != source['sha256'] or row['sha256'] != source['sha256']):
        raise ValueError('Exact publisher blob, size or frozen artifact checksum differs')
    added = identity not in known
    if added:
        prior['reviewed_artifacts'].append({**row,
            'proof': 'Google publisher-maintained AOSP source copy '+source['url']+'; Git blob '+blob+'; exact SHA256 '+source['sha256'],
            'proof_kind': 'PUBLISHER_MAINTAINED_SOURCE_COPY_NOT_PGP'})
    count = len(prior['reviewed_artifacts'])
    prior.update(reviewed_artifact_count=count, remaining_unreviewed_artifact_count=len(rows)-count,
                 complete_for_baseline=count == len(rows))
    prior['publisher_source_copy_reviews'] = prior.get('publisher_source_copy_reviews', [])+[{
        'registry_sha256': registry_pin, 'prior_review_sha256': prior_pin,
        'reviewer_sha256': batch.digest(Path(__file__).read_bytes()), 'identity': list(identity),
        'repository': REPOSITORY, 'commit': COMMIT, 'tag_object': TAG_OBJECT,
        'directory_sha256': source['directory_sha256'], 'blob_sha1': blob,
        'verified_new_files': int(added), 'pgp_signature_claim': False,
        'active_gradle_policy_changed': False}]
    return prior, batch.core.candidate_xml(prior['reviewed_artifacts'])


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    for arg in ('metadata', 'prior', 'registry', 'directory', 'pom', 'output'):
        p.add_argument(arg, type=Path)
    for arg in ('prior', 'registry'):
        p.add_argument('--expected-'+arg, required=True)
    a = p.parse_args()
    record, xml = review(batch.core.bounded_bytes(a.metadata, 3_000_000), batch.core.bounded_bytes(a.prior, 2_000_000),
                         batch.core.bounded_bytes(a.registry, 65536), batch.core.bounded_bytes(a.directory, 65536),
                         batch.core.bounded_bytes(a.pom, 262144), a.expected_prior, a.expected_registry)
    a.output.mkdir(parents=True, exist_ok=False)
    (a.output/'dependency-provenance-review.json').write_text(json.dumps(record, indent=2)+'\n')
    (a.output/'reviewed-verification-candidate.xml').write_bytes(xml)
    print(json.dumps({k: record[k] for k in ('reviewed_artifact_count', 'remaining_unreviewed_artifact_count', 'active_gradle_policy_changed')}))
