#!/usr/bin/env python3
"""Offline, pinned publisher review. Writes a PARTIAL candidate, never active Gradle trust."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile
import xml.etree.ElementTree as ET

NS = 'https://schema.gradle.org/dependency-verification'
KOTLIN_MANIFEST_SHA256 = '791a6bf5ff0404afe4f8eaf903faa6db5f97652f4d1e79b9795404f3111bb3c8'
JUNIT_KEYS_SHA256 = 'fcb938a928b643fb76985f9ce3634d9dad170dfe6c1b276e920844822538d5f7'
JUNIT_FINGERPRINT = 'FF6E2C001948C5F2F38B0CC385911F425EC61B51'


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def publisher_manifest(data):
    if sha256(data) != KOTLIN_MANIFEST_SHA256:
        raise ValueError('Kotlin publisher manifest differs from the inspected release asset')
    result = {}
    for line in data.decode('utf-8').splitlines():
        path, value = line.split()
        if path in result or path.startswith('/') or any(p in ('', '.', '..') for p in path.split('/')) or re.fullmatch('[0-9a-f]{64}', value) is None:
            raise ValueError('Ambiguous publisher manifest entry')
        result[path] = value
    return result


def valid_signer(returncode, status):
    if returncode != 0 or any(word in status for word in ('BADSIG', 'ERRSIG', 'EXPKEYSIG', 'REVKEYSIG', 'EXPSIG')):
        raise ValueError('JUnit signature verification failed')
    valid = [line.split() for line in status.splitlines() if line.startswith('[GNUPG:] VALIDSIG ')]
    if len(valid) != 1 or len(valid[0]) < 3 or valid[0][2] != JUNIT_FINGERPRINT:
        raise ValueError('JUnit signer differs from the publisher-maintained release key')
    return ' '.join(valid[0])


def verify_junit(key_data, signature, jar):
    if sha256(key_data) != JUNIT_KEYS_SHA256:
        raise ValueError('JUnit publisher KEYS differs from the inspected source')
    text = key_data.decode('ascii')
    start = text.index('-----BEGIN PGP PUBLIC KEY BLOCK-----')
    end = text.index('-----END PGP PUBLIC KEY BLOCK-----') + len('-----END PGP PUBLIC KEY BLOCK-----')
    with tempfile.TemporaryDirectory(prefix='fixmyshot-public-review-') as directory:
        root = Path(directory)
        root.chmod(0o700)
        public = root/'publisher.asc'
        public.write_text(text[start:end]+'\n')
        ring = root/'publisher.gpg'
        subprocess.run(['gpg', '--no-options', '--homedir', directory, '--batch', '--dearmor',
                        '--output', str(ring), str(public)], capture_output=True, check=True)
        sig, binary = root/'junit.asc', root/'junit.jar'
        sig.write_bytes(signature)
        binary.write_bytes(jar)
        checked = subprocess.run(['gpgv', '--homedir', directory, '--keyring', str(ring),
                                  '--status-fd', '1', str(sig), str(binary)], capture_output=True, text=True)
        return valid_signer(checked.returncode, checked.stdout)


def captured_rows(data):
    if len(data) > 3_000_000:
        raise ValueError('Oversized baseline metadata')
    result = []
    for component in ET.fromstring(data).findall('{'+NS+'}components/{'+NS+'}component'):
        for artifact in component.findall('{'+NS+'}artifact'):
            sums = artifact.findall('{'+NS+'}sha256')
            if len(sums) != 1 or re.fullmatch('[0-9a-f]{64}', sums[0].attrib['value']) is None:
                raise ValueError('Ambiguous captured checksum')
            result.append({**component.attrib, 'file': artifact.attrib['name'], 'sha256': sums[0].attrib['value']})
    identities = {(x['group'], x['name'], x['version'], x['file']) for x in result}
    if not result or len(identities) != len(result):
        raise ValueError('Missing or duplicate captured artifacts')
    return result


def candidate_xml(rows):
    root = ET.Element('verification-metadata', xmlns=NS)
    root.append(ET.Comment('PARTIAL REVIEWED CANDIDATE ONLY; not active verification-metadata.xml'))
    config = ET.SubElement(root, 'configuration')
    ET.SubElement(config, 'verify-metadata').text = 'true'
    ET.SubElement(config, 'verify-signatures').text = 'false'
    components = ET.SubElement(root, 'components')
    groups = {}
    for row in rows:
        groups.setdefault((row['group'], row['name'], row['version']), []).append(row)
    for (group, name, version), artifacts in sorted(groups.items()):
        component = ET.SubElement(components, 'component', group=group, name=name, version=version)
        for row in sorted(artifacts, key=lambda x: x['file']):
            artifact = ET.SubElement(component, 'artifact', name=row['file'])
            ET.SubElement(artifact, 'sha256', value=row['sha256'], origin=row['proof'])
    ET.indent(root, space='  ')
    return ET.tostring(root, encoding='utf-8', xml_declaration=True) + b'\n'


def review(evidence, metadata, expected_control):
    request = json.loads((evidence/'request.json').read_text())
    report = json.loads((evidence/'report.json').read_text())
    if re.fullmatch('[0-9a-f]{40}', expected_control or '') is None or report['control_commit'] != expected_control or report['all_required_pins_matched'] is not True:
        raise ValueError('Evidence control identity or completeness mismatch')
    if (request['baseline_source_commit'] != report['baseline_source_commit'] or
            sha256(metadata) != request['baseline_metadata_sha256'] or
            report['baseline_metadata_sha256'] != request['baseline_metadata_sha256']):
        raise ValueError('Baseline metadata identity mismatch')
    blobs = {}
    for item in request['entries']:
        if re.fullmatch('[a-z0-9-]{1,64}', item['id']) is None or item['id'] in blobs:
            raise ValueError('Invalid evidence identity')
        path = evidence/(item['id']+'.bin')
        if path.is_symlink() or not path.is_file() or path.stat().st_size > item['max_bytes']:
            raise ValueError('Invalid or oversized evidence file')
        data = path.read_bytes()
        if len(data) > item['max_bytes'] or sha256(data) != item['sha256']:
            raise ValueError('Evidence bytes differ from pinned request')
        blobs[item['id']] = data
    manifest = publisher_manifest(blobs['kotlin-publisher-checksums'])
    rows = captured_rows(metadata)
    approved = []
    for row in rows:
        path = row['group'].replace('.', '/')+'/'+row['name']+'/'+row['version']+'/'+row['file']
        if path in manifest:
            if manifest[path] != row['sha256']:
                raise ValueError('Captured artifact conflicts with the publisher release manifest')
            approved.append({**row, 'proof': 'Kotlin v2.2.21 official Maven SHA256 manifest '+KOTLIN_MANIFEST_SHA256})
    junit = [x for x in request['entries'] if x.get('coordinate') == 'junit:junit:4.13.2']
    if len(junit) != 1:
        raise ValueError('Missing exact JUnit artifact')
    item = junit[0]
    matched = [x for x in rows if (x['group'], x['name'], x['version'], x['file'], x['sha256']) ==
               ('junit', 'junit', '4.13.2', 'junit-4.13.2.jar', item['sha256'])]
    if len(matched) != 1:
        raise ValueError('JUnit baseline artifact identity mismatch')
    signature = (evidence/(item['id']+'.asc')).read_bytes()
    status = verify_junit(blobs['junit-publisher-keys'], signature, blobs[item['id']])
    approved.append({**matched[0], 'proof': 'JUnit detached PGP; publisher KEYS fingerprint '+JUNIT_FINGERPRINT})
    result = {'schema': 1, 'status': 'PARTIAL_PUBLISHER_REVIEW__NOT_ACTIVE',
              'evidence_control_commit': expected_control, 'baseline_source_commit': request['baseline_source_commit'],
              'baseline_metadata_sha256': request['baseline_metadata_sha256'],
              'review_script_sha256': sha256(Path(__file__).read_bytes()),
              'captured_artifact_count': len(rows), 'reviewed_artifact_count': len(approved),
              'remaining_unreviewed_artifact_count': len(rows)-len(approved),
              'complete_for_baseline': len(approved) == len(rows), 'active_gradle_policy_changed': False,
              'kotlin_manifest_sha256': KOTLIN_MANIFEST_SHA256,
              'junit_keys_sha256': JUNIT_KEYS_SHA256, 'junit_signing_fingerprint': JUNIT_FINGERPRINT,
              'junit_signature_sha256': sha256(signature), 'junit_validsig_status': status,
              'current_vulnerability_review': 'OPEN', 'reviewed_artifacts': approved}
    return result, candidate_xml(approved)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('evidence', type=Path)
    parser.add_argument('metadata', type=Path)
    parser.add_argument('output', type=Path)
    parser.add_argument('--expected-control', required=True)
    args = parser.parse_args()
    result, xml = review(args.evidence, args.metadata.read_bytes(), args.expected_control)
    args.output.mkdir(parents=True, exist_ok=False)
    (args.output/'dependency-provenance-review.json').write_text(json.dumps(result, indent=2)+'\n')
    (args.output/'reviewed-verification-candidate.xml').write_bytes(xml)
    print(json.dumps({k: v for k, v in result.items() if k != 'reviewed_artifacts'}, indent=2))


if __name__ == '__main__':
    main()
