#!/usr/bin/env python3
"""Bounded, data-only publisher evidence collection. Never approves trust or executes downloads."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile
import time
from urllib.error import HTTPError
from urllib.parse import urlsplit
from urllib.request import Request, build_opener, HTTPRedirectHandler

HOSTS = {'dl.google.com', 'repo.maven.apache.org', 'github.com',
         'raw.githubusercontent.com', 'release-assets.githubusercontent.com', 'objects.githubusercontent.com', 'keys.openpgp.org'}
PUBLIC_KEY_PATHS = {'/vks/v1/by-fingerprint/'+fingerprint for fingerprint in (
    '2FBA29D08D2E25EE84C132C30729A0AFF8999A87', 'DBD744ACE7ADE6AA50DD591F66B50994442D2D40')}

MAX_ITEM = 32 * 1024 * 1024
MAX_TOTAL = 256 * 1024 * 1024


def valid_url(url, *, redirect=False):
    parsed = urlsplit(url)
    if parsed.scheme != 'https' or parsed.hostname not in HOSTS or parsed.port not in (None, 443) or parsed.username or parsed.password or parsed.fragment or (parsed.query and not redirect):
        raise ValueError('Unapproved publisher URL')
    if parsed.hostname == 'keys.openpgp.org' and (parsed.path not in PUBLIC_KEY_PATHS or parsed.query):
        raise ValueError('Uninspected public-key lookup')
    return url


class PublisherRedirect(HTTPRedirectHandler):
    def redirect_request(self, request, fp, code, message, headers, newurl):
        valid_url(newurl, redirect=True)
        return super().redirect_request(request, fp, code, message, headers, newurl)


def read_policy(path):
    if path.stat().st_size > 65536:
        raise ValueError('Oversized evidence request')
    def unique(pairs):
        result = {}
        for key, value in pairs:
            if key in result:
                raise ValueError('Duplicate evidence field')
            result[key] = value
        return result
    policy = json.loads(path.read_text(), object_pairs_hook=unique)
    if set(policy) != {'schema', 'baseline_source_commit', 'baseline_metadata_sha256', 'entries'} or type(policy['schema']) is not int or policy['schema'] != 1:
        raise ValueError('Unknown evidence schema')
    for key, length in (('baseline_source_commit', 40), ('baseline_metadata_sha256', 64)):
        if not isinstance(policy[key], str) or re.fullmatch('[0-9a-f]{'+str(length)+'}', policy[key]) is None:
            raise ValueError('Invalid baseline pin')
    if not isinstance(policy['entries'], list) or not 1 <= len(policy['entries']) <= 128:
        raise ValueError('Invalid evidence count')
    seen = set()
    total = 0
    for item in policy['entries']:
        key_entry = item.get('kind') == 'publisher-key'
        artifact_entry = item.get('kind') in ('artifact', 'signed-artifact')
        fields = {'id', 'kind', 'url', 'max_bytes', 'primary_fingerprint' if key_entry else 'sha256'} | ({'coordinate'} if artifact_entry else set()) | ({'file'} if item.get('kind') == 'signed-artifact' else set())
        if set(item) != fields or item['kind'] not in {'artifact', 'signed-artifact', 'publisher', 'publisher-key'}:
            raise ValueError('Invalid evidence entry')
        if re.fullmatch('[a-z0-9-]{1,64}', item['id']) is None or item['id'] in seen:
            raise ValueError('Unsafe or duplicate evidence ID')
        seen.add(item['id'])
        if item['kind'] == 'signed-artifact' and (not isinstance(item['file'], str) or re.fullmatch('[A-Za-z0-9_+.-]{1,200}', item['file']) is None or item['file'] in ('.', '..')):
            raise ValueError('Invalid baseline artifact filename')
        valid_url(item['url'])
        pin = item['primary_fingerprint'] if key_entry else item['sha256']
        pattern = '[0-9A-F]{40}' if key_entry else '[0-9a-f]{64}'
        limit = 128 * 1024 if key_entry else MAX_ITEM
        if not isinstance(pin, str) or re.fullmatch(pattern, pin) is None or type(item['max_bytes']) is not int or not 0 < item['max_bytes'] <= limit:
            raise ValueError('Invalid evidence digest or size')
        total += item['max_bytes'] + (128 * 1024 if item['kind'] == 'signed-artifact' else 0)
    if total > MAX_TOTAL:
        raise ValueError('Oversized evidence batch')
    return policy


def fetch(url, limit, deadline):
    valid_url(url)
    remaining = deadline - time.monotonic()
    if remaining <= 0:
        raise TimeoutError('Evidence deadline reached')
    opener = build_opener(PublisherRedirect())
    with opener.open(Request(url, headers={'User-Agent': 'FixMyShot-provenance/1'}), timeout=min(15, remaining)) as response:
        valid_url(response.geturl(), redirect=True)
        data = bytearray()
        while True:
            if time.monotonic() >= deadline:
                raise TimeoutError('Evidence deadline reached')
            block = response.read(64 * 1024)
            if not block:
                return bytes(data)
            if len(data) + len(block) > limit:
                raise ValueError('Publisher response exceeds limit')
            data.extend(block)


def optional(url, limit, deadline, getter):
    try:
        return getter(url, limit, deadline), 'CAPTURED'
    except HTTPError as error:
        return None, 'HTTP_' + str(error.code)
    except (OSError, ValueError):
        return None, 'UNAVAILABLE'


def primary_fingerprints(listing):
    result, primary = [], False
    for line in listing.splitlines():
        fields = line.split(':')
        if fields[0] in ('pub', 'sub'):
            primary = fields[0] == 'pub'
        elif fields[0] == 'fpr' and primary:
            if len(fields) < 10 or re.fullmatch('[0-9A-F]{40}', fields[9]) is None or fields[9] in result:
                raise ValueError('Invalid or duplicate primary key identity')
            result.append(fields[9])
            primary = False
    if not result:
        raise ValueError('No public primary key identity')
    return result


def public_key_identity(data):
    # Public-only inspection in a disposable context, never a user keyring or keyserver.
    with tempfile.TemporaryDirectory(prefix='fixmyshot-public-identity-') as directory:
        root = Path(directory)
        root.chmod(0o700)
        key = root/'publisher.asc'
        key.write_bytes(data)
        checked = subprocess.run(['gpg', '--no-options', '--homedir', directory, '--batch',
                                  '--with-colons', '--show-keys', str(key)],
                                 capture_output=True, text=True, timeout=10, check=True)
        return primary_fingerprints(checked.stdout)


def collect(policy, output, control_commit, getter=fetch):
    if re.fullmatch('[0-9a-f]{40}', control_commit or '') is None:
        raise ValueError('Invalid controller commit')
    output.mkdir(parents=True, exist_ok=False)
    deadline = time.monotonic() + 120
    report = {'schema': 1, 'scope': 'PUBLISHER_EVIDENCE_ONLY', 'control_commit': control_commit,
              'baseline_source_commit': policy['baseline_source_commit'],
              'baseline_metadata_sha256': policy['baseline_metadata_sha256'],
              'automatically_authenticated': False, 'results': []}
    passed = True
    for item in policy['entries']:
        result = {'request': item, 'status': 'UNAVAILABLE', 'authenticated': False}
        report['results'].append(result)
        try:
            data = getter(item['url'], item['max_bytes'], deadline)
            if len(data) > item['max_bytes']:
                raise ValueError('Oversized response')
            sha = hashlib.sha256(data).hexdigest()
            (output / (item['id'] + '.bin')).write_bytes(data)
            if item['kind'] == 'publisher-key':
                fingerprints = public_key_identity(data)
                matched = item['primary_fingerprint'] in fingerprints
                result.update(sha256=sha, bytes=len(data), primary_fingerprints=fingerprints,
                              status='MATCHED_PRIMARY_FINGERPRINT' if matched else 'PRIMARY_FINGERPRINT_MISMATCH',
                              cryptographically_verified=False)
                passed = passed and matched
                continue
            result.update(sha256=sha, bytes=len(data), status='MATCHED_PIN' if sha == item['sha256'] else 'SHA256_MISMATCH')
            if sha != item['sha256']:
                passed = False
                continue
            if item['kind'] not in ('artifact', 'signed-artifact'):
                continue
            for algorithm in (() if item['kind'] == 'signed-artifact' else ('sha256', 'sha512', 'sha1')):
                raw, state = optional(item['url'] + '.' + algorithm, 4096, deadline, getter)
                result['published_checksum'] = {'algorithm': algorithm, 'status': state}
                if raw is None:
                    continue
                (output / (item['id'] + '.' + algorithm)).write_bytes(raw)
                value = raw.decode('ascii').strip().split()[0]
                length = hashlib.new(algorithm).digest_size * 2
                matched = re.fullmatch('[0-9a-fA-F]{'+str(length)+'}', value) is not None and value.lower() == hashlib.new(algorithm, data).hexdigest()
                result['published_checksum']['status'] = 'MATCHED' if matched else 'MISMATCH'
                if not matched:
                    passed = False
                break
            signature, state = optional(item['url'] + '.asc', 128 * 1024, deadline, getter)
            result['signature'] = {'status': state, 'cryptographically_verified': False}
            if signature is not None:
                (output / (item['id'] + '.asc')).write_bytes(signature)
                result['signature']['sha256'] = hashlib.sha256(signature).hexdigest()
        except HTTPError as error:
            result['status'] = 'HTTP_' + str(error.code)
            passed = False
        except (OSError, ValueError, IndexError, subprocess.SubprocessError):
            result['status'] = 'FETCH_OR_VALIDATION_ERROR'
            passed = False
    report['all_required_pins_matched'] = passed
    (output / 'request.json').write_text(json.dumps(policy, indent=2) + '\n')
    (output / 'report.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('request', type=Path)
    parser.add_argument('output', type=Path)
    parser.add_argument('--control-commit', required=True)
    args = parser.parse_args()
    report = collect(read_policy(args.request), args.output, args.control_commit)
    print(json.dumps(report, indent=2))
    if not report['all_required_pins_matched']:
        raise SystemExit('FAIL: incomplete or mismatched evidence; report retained, trust not approved')


if __name__ == '__main__':
    main()
