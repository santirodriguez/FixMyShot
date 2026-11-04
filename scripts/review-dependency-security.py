#!/usr/bin/env python3
"""Replay pinned captured OSV responses offline and emit a compact, non-approving receipt."""
import argparse
from collections import Counter
import importlib.util
import json
from pathlib import Path
import re

spec = importlib.util.spec_from_file_location('audit', Path(__file__).with_name('audit-dependency-security.py'))
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


def bounded(path):
    if path.is_symlink() or not path.is_file() or path.stat().st_size > 3_000_000:
        raise ValueError('Missing, linked or oversized security evidence')
    return path.read_bytes()


def normalized(row):
    row = dict(row)
    # Original capture used this misleading label. Absence from a project
    # lockfile does not establish deselection of buildscript/plugin dependencies.
    if row['scope'] == 'capture_only_unselected':
        row['scope'] = 'capture_without_lockfile_selection'
    return row


def reconcile(rows, report, responses, details):
    if len(responses) != len(rows) or len(report['components']) != len(rows):
        raise ValueError('Incomplete replay inventory')
    result = []
    for expected, captured, match in zip(rows, report['components'], responses):
        captured = normalized(captured)
        if any(captured.get(k) != v for k, v in expected.items()):
            raise ValueError('Captured component scope or query identity changed')
        if match['incomplete'] or captured.get('query_complete') is not True or captured.get('database_match_ids') != match['ids']:
            raise ValueError('Incomplete query or changed database matches')
        if any(v not in details for v in match['ids']):
            raise ValueError('Missing advisory details')
        active = [v for v in match['ids'] if not details[v].get('withdrawn')]
        status = ('POTENTIAL_DATABASE_MATCH_REQUIRES_TRIAGE' if active else
                  'WITHDRAWN_MATCHES_ONLY' if match['ids'] else 'NO_DATABASE_MATCH_AT_SCAN_TIME')
        if captured.get('active_match_ids') != active or captured.get('status') != status:
            raise ValueError('Captured conclusion differs from offline replay')
        result.append({'coordinate': expected['coordinate'], 'scope': expected['scope'],
                       'database_match_ids': match['ids'], 'active_match_ids': active, 'status': status})
    return result


def review(root, evidence, control, request_pin, report_pin):
    if not re.fullmatch('[0-9a-f]{40}', control or ''):
        raise ValueError('Uninspected security control')
    raw_request, raw_report = bounded(evidence/'request.json'), bounded(evidence/'report.json')
    if audit.digest(raw_request) != request_pin or audit.digest(raw_report) != report_pin:
        raise ValueError('Security request/report differs from the inspected capture pin')
    policy, report = json.loads(raw_request), json.loads(raw_report)
    rows = audit.inventory(root, policy)
    if (report.get('control_commit') != control or report.get('request_sha256') != request_pin or
            report.get('inputs') != policy['inputs'] or report.get('baseline_source_commit') != policy['baseline_source_commit'] or
            report.get('native_commit') != policy['native_commit'] or report.get('security_approval') is not False or
            report.get('current_vulnerability_review') != 'OPEN' or report.get('errors') != [] or
            report.get('status') != 'DATABASE_INVENTORY_COMPLETE__TRIAGE_REQUIRED' or
            report.get('queried_component_count') != len(rows) or report.get('inventory_component_count') != len(rows)):
        raise ValueError('Security capture control, scope or completion mismatch')
    captures = {}
    for item in report['captures']:
        name = item['file']
        if not re.fullmatch('[A-Za-z0-9_.-]+', name) or name in captures:
            raise ValueError('Unsafe or duplicate capture filename')
        data = bounded(evidence/name)
        if len(data) != item['bytes'] or audit.digest(data) != item['sha256']:
            raise ValueError('Captured security bytes differ')
        captures[name] = json.loads(data)
    if captures.get('request.json') != policy or [normalized(x) for x in captures.get('inventory.json', [])] != rows:
        raise ValueError('Captured input inventory differs from pinned source files')
    responses, expected_files = [], {'request.json', 'inventory.json'}
    for start in range(0, len(rows), 100):
        name = 'batch-'+str(start//100).zfill(2)
        expected_files.update({name+'-request.json', name+'-response.json'})
        batch = rows[start:start+100]
        if captures.get(name+'-request.json') != {'queries': [x['query'] for x in batch]}:
            raise ValueError('Captured query batch differs from baseline identities/order')
        responses.extend(audit.matches(captures.get(name+'-response.json', {}), len(batch)))
    ids = sorted({v for match in responses for v in match['ids']})
    details = {}
    for advisory_id in ids:
        name = 'advisory-'+advisory_id+'-response.json'
        expected_files.add(name)
        detail = captures.get(name, {})
        if detail.get('id') != advisory_id:
            raise ValueError('Missing or mismatched advisory identity')
        details[advisory_id] = detail
    if set(captures) != expected_files:
        raise ValueError('Unexpected or missing security capture files')
    components = reconcile(rows, report, responses, details)
    advisories = []
    for advisory_id, detail in sorted(details.items()):
        packages = {x['coordinate'].rsplit(':', 1)[0] for x in components if advisory_id in x['database_match_ids']}
        advisories.append({'id': advisory_id, 'aliases': detail.get('aliases', []), 'withdrawn': bool(detail.get('withdrawn')),
                           'modified': detail.get('modified'), 'database_severity': detail.get('database_specific', {}).get('severity'),
                           'url': 'https://osv.dev/vulnerability/'+advisory_id,
                           'matched_package_ranges': [{'package': x['package']['name'], 'ranges': x.get('ranges', [])}
                               for x in detail.get('affected', []) if x['package'].get('ecosystem') == 'Maven' and x['package']['name'] in packages]})
    alias_groups = []
    for detail in advisories:
        group = {detail['id'], *detail['aliases']}
        overlaps = [x for x in alias_groups if x & group]
        for other in overlaps:
            group |= other
            alias_groups.remove(other)
        alias_groups.append(group)
    return {'schema': 1, 'status': 'OFFLINE_REPLAY_PASS__SECURITY_REVIEW_OPEN', 'security_approval': False,
            'control_commit': control, 'request_sha256': request_pin, 'captured_report_sha256': report_pin,
            'review_script_sha256': audit.digest(Path(__file__).read_bytes()), 'scanned_at_utc': report['scanned_at_utc'],
            'inputs': policy['inputs'], 'component_count': len(rows), 'scope_counts': dict(Counter(x['scope'] for x in components)),
            'matching_component_count': sum(bool(x['active_match_ids']) for x in components),
            'matching_scope_counts': dict(Counter(x['scope'] for x in components if x['active_match_ids'])),
            'advisory_count': len(advisories), 'advisory_alias_groups': [sorted(x) for x in alias_groups],
            'advisories': advisories, 'components': components, 'capture_receipts': report['captures'],
            'scope_note': 'Lockfile absence does not establish buildscript/plugin deselection. Database matches are not exploitability verdicts.'}


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('evidence', type=Path)
    parser.add_argument('output', type=Path)
    for arg in ('control', 'request', 'report'):
        parser.add_argument('--expected-'+arg, required=True)
    args = parser.parse_args()
    result = review(Path('.'), args.evidence, args.expected_control, args.expected_request, args.expected_report)
    with args.output.open('xb') as stream:
        stream.write(audit.encoded(result))
    print(json.dumps({k: result[k] for k in ('status', 'component_count', 'matching_component_count', 'matching_scope_counts', 'advisory_count')}))
