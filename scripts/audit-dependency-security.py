#!/usr/bin/env python3
"""Bounded OSV inventory. Database matches require triage; this never grants trust."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
import time
import urllib.request
import xml.etree.ElementTree as ET

BASELINE = 'docs/feasibility/evidence/gradle-baseline-773b34f0.xml'
LOCKS = ('app/gradle.lockfile', 'core/gradle.lockfile', 'codec-native/gradle.lockfile')
NATIVE = 'codec-native/src/main/cpp/CMakeLists.txt'
NS = '{https://schema.gradle.org/dependency-verification}'
API = 'https://api.osv.dev/v1/'


def digest(data):
    return hashlib.sha256(data).hexdigest()


def encoded(value):
    return (json.dumps(value, indent=2, sort_keys=True)+'\n').encode()


def read_pin(root, path, value):
    file = root/path
    if file.is_symlink() or not file.is_file() or file.stat().st_size > 3_000_000:
        raise ValueError('Missing, linked or oversized pinned input')
    data = file.read_bytes()
    if not re.fullmatch('[0-9a-f]{64}', value or '') or digest(data) != value:
        raise ValueError('Pinned inventory input differs')
    return data


def inventory(root, policy):
    if (policy.get('schema') != 1 or set(policy['inputs']) != {BASELINE, *LOCKS, NATIVE} or
            not re.fullmatch('[0-9a-f]{40}', policy['baseline_source_commit']) or
            not re.fullmatch('[0-9a-f]{40}', policy['native_commit'])):
        raise ValueError('Unsupported inventory scope')
    inputs = {p: read_pin(root, p, v) for p, v in policy['inputs'].items()}
    if not re.search(rb'GIT_TAG\s+'+policy['native_commit'].encode()+rb'\s', inputs[NATIVE]):
        raise ValueError('Native source pin differs')
    selected = {}
    for path in LOCKS:
        for line in inputs[path].decode().splitlines():
            if not line or line.startswith('#') or line.startswith('empty='):
                continue
            coordinate, configurations = line.split('=', 1)
            if len(coordinate.split(':')) != 3:
                raise ValueError('Ambiguous locked coordinate')
            selected.setdefault(coordinate, []).append({'lockfile': path, 'configurations': configurations.split(',')})
    components = ET.fromstring(inputs[BASELINE]).findall(NS+'components/'+NS+'component')
    coordinates = sorted({':'.join(x.attrib[k] for k in ('group', 'name', 'version')) for x in components})
    if len(coordinates) != len(components) or not coordinates:
        raise ValueError('Empty or duplicate baseline components')
    rows = []
    for coordinate in coordinates:
        group, name, version = coordinate.split(':')
        associations = selected.get(coordinate, [])
        release = any(x['lockfile'] == LOCKS[0] and 'releaseRuntimeClasspath' in x['configurations'] for x in associations)
        scope = 'app_release_runtime' if release else ('selected_other_configuration' if associations else 'capture_only_unselected')
        rows.append({'coordinate': coordinate, 'scope': scope, 'lock_associations': associations,
                     'query': {'package': {'name': group+':'+name, 'ecosystem': 'Maven'}, 'version': version}})
    rows.append({'coordinate': 'webmproject/libwebp@'+policy['native_commit'], 'scope': 'native_runtime_source',
                 'query': {'commit': policy['native_commit']}})
    return rows


def matches(response, count):
    results = response.get('results')
    if not isinstance(results, list) or len(results) != count:
        raise ValueError('Incomplete or reordered OSV result count')
    parsed = []
    for result in results:
        if not isinstance(result, dict) or set(result)-{'vulns', 'next_page_token'}:
            raise ValueError('Unexpected OSV result structure')
        vulns = result.get('vulns', [])
        if not isinstance(vulns, list) or any(not isinstance(v, dict) or
                not re.fullmatch('[A-Za-z0-9_.-]{1,160}', v.get('id', '')) for v in vulns):
            raise ValueError('Malformed OSV matches')
        parsed.append({'ids': sorted({v['id'] for v in vulns}), 'incomplete': bool(result.get('next_page_token'))})
    return parsed


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        raise ValueError('OSV redirect refused')


class Capture:
    def __init__(self, output):
        self.output, self.deadline, self.remaining, self.files = output, time.monotonic()+180, 16*1024*1024, []
        self.opener = urllib.request.build_opener(NoRedirect)

    def request(self, path, label, payload=None):
        remaining_time = self.deadline-time.monotonic()
        if remaining_time <= 0 or self.remaining <= 0:
            raise ValueError('OSV collection budget exhausted')
        request_data = encoded(payload) if payload is not None else None
        if request_data is not None:
            self.save(label+'-request.json', request_data)
        request = urllib.request.Request(API+path, data=request_data,
                                         headers={'Content-Type': 'application/json', 'User-Agent': 'FixMyShot-evidence/1'})
        with self.opener.open(request, timeout=min(25, remaining_time)) as response:
            if response.status != 200 or response.geturl() != API+path:
                raise ValueError('Unexpected OSV response origin/status')
            data = response.read(min(2*1024*1024, self.remaining)+1)
        if len(data) > min(2*1024*1024, self.remaining):
            raise ValueError('OSV response exceeds capture budget')
        self.remaining -= len(data)
        self.save(label+'-response.json', data)
        return json.loads(data)

    def save(self, name, data):
        (self.output/name).write_bytes(data)
        self.files.append({'file': name, 'sha256': digest(data), 'bytes': len(data)})


def collect(root, policy_path, output, control):
    if not re.fullmatch('[0-9a-f]{40}', control or ''):
        raise ValueError('Missing exact collection control')
    raw_policy = policy_path.read_bytes()
    policy = json.loads(raw_policy)
    active = (root/'.ci/dependency-provenance-request.json').read_bytes()
    output.mkdir(parents=True, exist_ok=False)
    if digest(active) != policy['paired_provenance_request_sha256']:
        (output/'report.json').write_bytes(encoded({'status': 'SKIPPED_UNPAIRED_REQUEST', 'network_requests': 0,
                                                  'control_commit': control, 'security_approval': False}))
        return True
    rows = inventory(root, policy)
    capture = Capture(output)
    capture.save('request.json', raw_policy)
    capture.save('inventory.json', encoded(rows))
    errors, details, complete = [], {}, True
    try:
        for start in range(0, len(rows), 100):
            batch = rows[start:start+100]
            response = capture.request('querybatch', 'batch-'+str(start//100).zfill(2), {'queries': [x['query'] for x in batch]})
            for row, match in zip(batch, matches(response, len(batch))):
                row['database_match_ids'] = match['ids']
                row['query_complete'] = not match['incomplete']
                complete &= row['query_complete']
        ids = sorted({v for row in rows for v in row.get('database_match_ids', [])})
        if len(ids) > 100:
            complete = False
            errors.append('DETAIL_CAP_EXCEEDED')
        for advisory_id in ids[:100]:
            detail = capture.request('vulns/'+advisory_id, 'advisory-'+advisory_id)
            if detail.get('id') != advisory_id:
                raise ValueError('OSV detail identity mismatch')
            details[advisory_id] = {'withdrawn': bool(detail.get('withdrawn')), 'aliases': detail.get('aliases', []),
                                    'summary': detail.get('summary', '')}
    except (ValueError, OSError, TimeoutError) as error:
        complete = False
        errors.append(type(error).__name__)  # no credential-bearing or remote error text
    for row in rows:
        row['active_match_ids'] = [v for v in row.get('database_match_ids', []) if v in details and not details[v]['withdrawn']]
        row['status'] = ('NOT_QUERIED' if 'database_match_ids' not in row else
                         'INCOMPLETE_QUERY' if not row['query_complete'] else
                         'POTENTIAL_DATABASE_MATCH_REQUIRES_TRIAGE' if row['active_match_ids'] else
                         'DETAILS_INCOMPLETE' if any(v not in details for v in row['database_match_ids']) else
                         'WITHDRAWN_MATCHES_ONLY' if row['database_match_ids'] else 'NO_DATABASE_MATCH_AT_SCAN_TIME')
    report = {'schema': 1, 'status': 'DATABASE_INVENTORY_COMPLETE__TRIAGE_REQUIRED' if complete else 'INCOMPLETE__NO_APPROVAL',
              'control_commit': control, 'request_sha256': digest(raw_policy), 'inputs': policy['inputs'],
              'baseline_source_commit': policy['baseline_source_commit'], 'native_commit': policy['native_commit'],
              'scanned_at_utc': datetime.now(timezone.utc).isoformat(), 'security_approval': False,
              'current_vulnerability_review': 'OPEN', 'queried_component_count': sum('database_match_ids' in x for x in rows),
              'inventory_component_count': len(rows), 'details': details, 'errors': errors, 'components': rows, 'captures': capture.files}
    (output/'report.json').write_bytes(encoded(report))
    print(json.dumps({k: report[k] for k in ('status', 'inventory_component_count', 'queried_component_count', 'errors')}))
    return complete


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('request', type=Path)
    parser.add_argument('output', type=Path)
    parser.add_argument('--control-commit', required=True)
    args = parser.parse_args()
    raise SystemExit(0 if collect(Path('.'), args.request, args.output, args.control_commit) else 1)
