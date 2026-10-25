#!/usr/bin/env python3
"""Review exact JetBrains-owned files against a pinned official Kotlin release source checksum record."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location('batch', Path(__file__).with_name('review-google-batch.py'))
batch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(batch)
COMMIT = '9180ce6a7a7d3627f88b424717583f28ddca052d'
SOURCE_SHA256 = '60d44a5f54f4a439583db9808af334feb9a7468c17806f661449e3e34e3a042a'
SOURCE_BLOB = '274270d7a46c4e1b6ce6734a32dca82721e58368'
SOURCE_URL = 'https://github.com/JetBrains/kotlin/blob/'+COMMIT+'/gradle/verification-metadata.xml'


def review(metadata, prior_bytes, publisher_bytes, prior_pin):
    prior, rows, known = batch.checked_receipt(prior_bytes, metadata, prior_pin)
    if (len(publisher_bytes) > 1_000_000 or batch.digest(publisher_bytes) != SOURCE_SHA256 or
            hashlib.sha1(b'blob '+str(len(publisher_bytes)).encode()+b'\0'+publisher_bytes).hexdigest() != SOURCE_BLOB or
            b'<!DOCTYPE' in publisher_bytes.upper() or b'<!ENTITY' in publisher_bytes.upper()):
        raise ValueError('Official release source checksum record differs from inspected pins')
    published = {}
    for component in ET.fromstring(publisher_bytes).findall('.//{*}component'):
        gav = tuple(component.attrib[k] for k in ('group', 'name', 'version'))
        # A publisher's checksum of a foreign dependency is not that dependency's issuer proof.
        if gav[0] != 'org.jetbrains.kotlinx' and gav[:2] != ('org.jetbrains', 'annotations'):
            continue
        for artifact in component.findall('{*}artifact'):
            identity = (*gav, artifact.attrib['name'])
            hashes = artifact.findall('{*}sha256')
            if identity in published or len(hashes) != 1 or list(hashes[0]):
                raise ValueError('Duplicate or ambiguous publisher checksum identity')
            published[identity] = hashes[0].attrib['value']
    approved = []
    for identity, row in sorted(rows.items()):
        if identity in known or identity not in published:
            continue
        if row['sha256'] != published[identity]:
            raise ValueError('Exact publisher source checksum conflicts with frozen artifact')
        approved.append({**row, 'proof_kind':'PUBLISHER_RELEASE_SOURCE_CHECKSUM_RECORD_NOT_PGP',
                         'proof':'JetBrains official Kotlin v2.4.21 release source '+SOURCE_URL+'; source SHA256 '+SOURCE_SHA256+'; exact artifact checksum '+row['sha256']})
    prior['reviewed_artifacts'].extend(approved)
    count = len(prior['reviewed_artifacts'])
    prior.update(reviewed_artifact_count=count, remaining_unreviewed_artifact_count=len(rows)-count,
                 complete_for_baseline=count == len(rows))
    prior['publisher_source_checksum_reviews'] = prior.get('publisher_source_checksum_reviews', [])+[{
        'repository':'JetBrains/kotlin', 'release_tag':'v2.4.21', 'commit':COMMIT,
        'source_url':SOURCE_URL, 'source_sha256':SOURCE_SHA256, 'source_blob_sha1':SOURCE_BLOB,
        'prior_review_sha256':prior_pin, 'reviewer_sha256':batch.digest(Path(__file__).read_bytes()),
        'verified_exact_files':[list((x['group'],x['name'],x['version'],x['file'])) for x in approved],
        'verified_new_files':len(approved), 'pgp_signature_claim':False,
        'namespace_wide_trust':False, 'active_gradle_policy_changed':False}]
    return prior, batch.core.candidate_xml(prior['reviewed_artifacts'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for arg in ('metadata','prior','publisher_source','output'):
        parser.add_argument(arg, type=Path)
    parser.add_argument('--expected-prior', required=True)
    args = parser.parse_args()
    record, xml = review(batch.core.bounded_bytes(args.metadata, 3_000_000),
                         batch.core.bounded_bytes(args.prior, 2_000_000),
                         batch.core.bounded_bytes(args.publisher_source, 1_000_000), args.expected_prior)
    args.output.mkdir(parents=True, exist_ok=False)
    (args.output/'dependency-provenance-review.json').write_text(json.dumps(record, indent=2)+'\n')
    (args.output/'reviewed-verification-candidate.xml').write_bytes(xml)
    print(json.dumps({'reviewed':record['reviewed_artifact_count'], 'pending':record['remaining_unreviewed_artifact_count'],
                      'new_verified':record['publisher_source_checksum_reviews'][-1]['verified_new_files'], 'active_policy_changed':False}))
