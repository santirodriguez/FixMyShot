#!/usr/bin/env python3
"""Review exact runtime-priority artifacts against separately pinned issuer anchors."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import subprocess
import tempfile

spec = importlib.util.spec_from_file_location('batch', Path(__file__).with_name('review-google-batch.py'))
batch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(batch)
core, digest = batch.core, batch.digest


def verify(key, signature, artifact, primary):
    with tempfile.TemporaryDirectory(prefix='fixmyshot-runtime-public-') as directory:
        root = Path(directory)
        root.chmod(0o700)
        for name, data in [('key.asc', key), ('signature.asc', signature), ('artifact.bin', artifact)]:
            (root/name).write_bytes(data)
        subprocess.run(['gpg', '--no-options', '--homedir', directory, '--batch', '--dearmor',
                        '--output', str(root/'key.gpg'), str(root/'key.asc')],
                       capture_output=True, check=True, timeout=10)
        listing = subprocess.run(['gpg', '--no-options', '--homedir', directory, '--batch', '--with-colons',
                                  '--show-keys', str(root/'key.asc')], capture_output=True, text=True, check=True, timeout=10)
        checked = subprocess.run(['gpgv', '--homedir', directory, '--keyring', str(root/'key.gpg'), '--status-fd', '1',
                                  str(root/'signature.asc'), str(root/'artifact.bin')], capture_output=True, text=True, timeout=10)
        return validate_status(checked.returncode, checked.stdout, listing.stdout, primary)


def validate_status(returncode, status, listing, primary):
    if returncode != 0 or any(x in status for x in
            ('BADSIG', 'ERRSIG', 'EXPKEYSIG', 'REVKEYSIG', 'EXPSIG', 'NO_PUBKEY', 'NODATA', 'FAILURE')):
        raise ValueError('Detached signature refused')
    valid = [x for x in status.splitlines() if x.startswith('[GNUPG:] VALIDSIG ')]
    tokens = valid[0].split() if len(valid) == 1 else []
    if len(tokens) != 12 or tokens[11] != primary or tokens[9] not in ('8', '9', '10'):
        raise ValueError('Missing strong signature bound to the anchored primary')
    core.validate_signing_key(listing, valid[0], primary)
    return valid[0]


def priority_rows(rows, known, lock):
    runtime = set()
    for line in lock.decode('utf-8').splitlines():
        if '=' in line and 'releaseRuntimeClasspath' in line.split('=', 1)[1].split(','):
            runtime.add(line.split('=', 1)[0])
    if not runtime:
        raise ValueError('Missing app runtime selection')
    return {identity: row for identity, row in rows.items() if identity not in known and
            ':'.join(identity[:3]) in runtime and
            not identity[0].startswith(('androidx.', 'com.android.'))}


def review(evidence, metadata, prior_bytes, registry_bytes, lock, control, request_pin, prior_pin, registry_pin, lock_pin):
    prior, rows, known = batch.checked_receipt(prior_bytes, metadata, prior_pin)
    if (not re.fullmatch('[0-9a-f]{40}', control or '') or digest(registry_bytes) != registry_pin or digest(lock) != lock_pin):
        raise ValueError('Uninspected control, publisher anchors or runtime selection')
    registry = json.loads(registry_bytes)
    if registry.get('schema') != 1 or registry.get('review_scope') != 'EXACT_BASELINE_RUNTIME_PRIORITY_ONLY':
        raise ValueError('Unsupported issuer registry')
    raw_request = core.bounded_bytes(evidence/'request.json', 65536)
    if digest(raw_request) != request_pin:
        raise ValueError('Capture request changed')
    request = batch.planner.collector.read_policy(evidence/'request.json')
    raw_report = core.bounded_bytes(evidence/'report.json', 524288)
    report = json.loads(raw_report)
    for obj in (registry, request, report):
        if any(obj.get(k) != prior[k] for k in ('baseline_source_commit', 'baseline_metadata_sha256')):
            raise ValueError('Baseline identity conflict')
    if (report.get('control_commit') != control or report.get('scope') != 'PUBLISHER_EVIDENCE_ONLY' or
            report.get('automatically_authenticated') is not False):
        raise ValueError('Capture controller or trust conflict')
    captures = report.get('results', [])
    if len(captures) != len(request['entries']) or any(x.get('request') != y or x.get('authenticated') is not False
            for x, y in zip(captures, request['entries'])):
        raise ValueError('Missing, reordered or automatically approved captures')
    captures = {x['request']['id']: x for x in captures}
    publishers, keys, key_receipts = {}, {}, []
    expected_keys = set()
    for publisher in registry['publishers']:
        primary, entry_id = publisher['primary_fingerprint'], publisher['key_entry']
        source = publisher['source_text'].encode()
        if (not re.fullmatch('[0-9A-F]{40}', primary) or not re.fullmatch('[0-9a-f]{40}', publisher['source_commit']) or
                hashlib.sha1(b'blob '+str(len(source)).encode()+b'\0'+source).hexdigest() != publisher['source_blob'] or
                primary not in publisher['source_text'].replace(' ', '').upper() or entry_id in expected_keys):
            raise ValueError('Invalid anchored publisher identity')
        expected_keys.add(entry_id)
        captured = captures.get(entry_id, {})
        item = captured.get('request', {})
        if (item.get('kind') != 'publisher-key' or item.get('primary_fingerprint') != primary or
                item.get('url') != 'https://keys.openpgp.org/vks/v1/by-fingerprint/'+primary):
            raise ValueError('Publisher key route or fingerprint changed')
        key_receipt = {'publisher': publisher['id'], 'primary_fingerprint': primary, 'capture_status': captured.get('status')}
        key_receipts.append(key_receipt)
        if captured.get('status') == 'MATCHED_PRIMARY_FINGERPRINT':
            key = core.bounded_bytes(evidence/(entry_id+'.bin'), 131072)
            if digest(key) != captured.get('sha256') or len(key) != captured.get('bytes'):
                raise ValueError('Captured public key changed')
            if batch.planner.collector.public_key_identity(key) != [primary]:
                raise ValueError('Key is not the sole anchored public primary')
            keys[entry_id] = key
            key_receipt['sha256'] = digest(key)
        for group in publisher['groups']:
            if group in publishers or not re.fullmatch('[A-Za-z0-9_.-]+', group):
                raise ValueError('Ambiguous issuer scope')
            publishers[group] = publisher
    priority = priority_rows(rows, known, lock)
    outcomes, approved, attempted = [], [], set()
    for item in request['entries']:
        if item['id'] in expected_keys:
            continue
        if item['kind'] != 'signed-artifact':
            raise ValueError('Unsupported runtime evidence type')
        identity = tuple(item['coordinate'].split(':')+[item['file']])
        if identity not in priority or identity in attempted or item['sha256'] != priority[identity]['sha256']:
            raise ValueError('Unknown, repeated or non-priority artifact')
        attempted.add(identity)
        row = priority[identity]
        url = 'https://repo.maven.apache.org/maven2/'+'/'.join((row['group'].replace('.', '/'), row['name'], row['version'], row['file']))
        if item['url'] != url:
            raise ValueError('Unexpected artifact origin')
        captured = captures[item['id']]
        outcome = {'identity': list(identity), 'sha256': row['sha256'], 'capture_status': captured.get('status')}
        outcomes.append(outcome)
        if captured.get('status') != 'MATCHED_PIN':
            outcome['status'] = 'CAPTURE_PIN_CONFLICT' if captured.get('status') == 'SHA256_MISMATCH' else 'CAPTURE_UNAVAILABLE'
            continue
        data = core.bounded_bytes(evidence/(item['id']+'.bin'), item['max_bytes'])
        if digest(data) != row['sha256'] or captured.get('sha256') != row['sha256'] or captured.get('bytes') != len(data):
            raise ValueError('Captured artifact bytes changed')
        sig_receipt = captured.get('signature', {})
        if sig_receipt.get('status') != 'CAPTURED':
            outcome['status'] = 'SIGNATURE_ABSENT'
            continue
        signature = core.bounded_bytes(evidence/(item['id']+'.asc'), 131072)
        if digest(signature) != sig_receipt.get('sha256'):
            raise ValueError('Captured signature changed')
        outcome['signature_sha256'] = digest(signature)
        publisher = publishers.get(row['group'])
        if not publisher:
            outcome['status'] = 'ISSUER_ANCHOR_UNAVAILABLE'
            continue
        key = keys.get(publisher['key_entry'])
        if key is None:
            outcome['status'] = 'KEY_CAPTURE_UNAVAILABLE'
            continue
        try:
            status = verify(key, signature, data, publisher['primary_fingerprint'])
        except (ValueError, subprocess.SubprocessError):
            outcome['status'] = 'SIGNATURE_UNVERIFIED'
            continue
        outcome.update(status='VERIFIED', validsig_status=status, publisher=publisher['id'])
        approved.append({**row, 'proof': publisher['id']+' detached PGP; primary '+publisher['primary_fingerprint']+'; signer '+status.split()[2]})
    if attempted != set(priority):
        raise ValueError('Incomplete runtime priority request')
    prior['reviewed_artifacts'].extend(approved)
    prior['reviewed_artifact_count'] += len(approved)
    prior['remaining_unreviewed_artifact_count'] -= len(approved)
    prior['complete_for_baseline'] = prior['remaining_unreviewed_artifact_count'] == 0
    prior.setdefault('runtime_publisher_batches', []).append({
        'control_commit': control, 'request_sha256': request_pin, 'report_sha256': digest(raw_report),
        'prior_review_sha256': prior_pin, 'registry_sha256': registry_pin, 'app_lock_sha256': lock_pin,
        'review_script_sha256': digest(Path(__file__).read_bytes()),
        'verifier_script_sha256': digest(Path(core.__file__).read_bytes()),
        'verified_file_count': len(approved), 'keys': key_receipts, 'outcomes': outcomes})
    return prior, core.candidate_xml(prior['reviewed_artifacts'])


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    for arg in ('evidence', 'metadata', 'prior', 'registry', 'lock', 'output'):
        parser.add_argument(arg, type=Path)
    for arg in ('control', 'request', 'prior', 'registry', 'lock'):
        parser.add_argument('--expected-'+arg, required=True)
    args = parser.parse_args()
    result, xml = review(args.evidence, core.bounded_bytes(args.metadata, 3000000), core.bounded_bytes(args.prior, 2000000),
                         core.bounded_bytes(args.registry, 65536), core.bounded_bytes(args.lock, 262144),
                         args.expected_control, args.expected_request, args.expected_prior, args.expected_registry, args.expected_lock)
    args.output.mkdir(parents=True, exist_ok=False)
    (args.output/'dependency-provenance-review.json').write_text(json.dumps(result, indent=2)+'\n')
    (args.output/'reviewed-verification-candidate.xml').write_bytes(xml)
    print(json.dumps({'reviewed': result['reviewed_artifact_count'], 'pending': result['remaining_unreviewed_artifact_count'],
                      'new_verified': result['runtime_publisher_batches'][-1]['verified_file_count'], 'active_policy_changed': False}))
