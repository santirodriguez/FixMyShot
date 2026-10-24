#!/usr/bin/env python3
"""Offline refusal and scope checks; never contacts OSV."""
import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('security', Path(__file__).with_name('audit-dependency-security.py'))
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)


class SecurityInventory(unittest.TestCase):
    def test_result_count_and_schema_fail_closed(self):
        for value in ({}, {'results': [{}]}, {'results': [{'error': 'unknown'}, {}]},
                      {'results': [{'vulns': [{'id': '../escape'}]}, {}]}):
            with self.assertRaises(ValueError):
                audit.matches(value, 2)

    def test_pagination_is_incomplete_and_empty_match_is_valid(self):
        self.assertEqual(audit.matches({'results': [{}, {'next_page_token': 'more'}]}, 2),
                         [{'ids': [], 'incomplete': False}, {'ids': [], 'incomplete': True}])

    def test_inventory_exact_scopes_and_native_pin(self):
        policy = json.loads(Path('.ci/dependency-security-request.json').read_text())
        rows = audit.inventory(Path('.'), policy)
        self.assertEqual(len(rows), 515)
        self.assertEqual(len({x['coordinate'] for x in rows}), len(rows))
        self.assertTrue(any(x['scope'] == 'capture_without_lockfile_selection' for x in rows))
        runtime = [x for x in rows if x['scope'] == 'app_release_runtime']
        self.assertTrue(runtime)
        self.assertTrue(all(any(a['lockfile'] == 'app/gradle.lockfile' and
                                'releaseRuntimeClasspath' in a['configurations'] for a in x['lock_associations']) for x in runtime))
        changed = copy.deepcopy(policy)
        changed['inputs'][audit.LOCKS[0]] = '0'*64
        with self.assertRaises(ValueError):
            audit.inventory(Path('.'), changed)
        changed = copy.deepcopy(policy)
        changed['native_commit'] = '0'*40
        with self.assertRaises(ValueError):
            audit.inventory(Path('.'), changed)

    def test_unpaired_request_has_no_network(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            (root/'.ci').mkdir()
            (root/'.ci/dependency-provenance-request.json').write_text('{}')
            policy = root/'policy.json'
            policy.write_text(json.dumps({'paired_provenance_request_sha256': '0'*64}))
            original = audit.Capture
            audit.Capture = lambda *_: self.fail('unpaired request created network client')
            try:
                self.assertTrue(audit.collect(root, policy, root/'out', 'a'*40))
                self.assertEqual(json.loads((root/'out/report.json').read_text())['network_requests'], 0)
            finally:
                audit.Capture = original

    def test_deadline_prevents_request(self):
        with tempfile.TemporaryDirectory() as folder:
            capture = audit.Capture(Path(folder))
            capture.deadline = 0
            with self.assertRaises(ValueError):
                capture.request('querybatch', 'test', {'queries': []})


if __name__ == '__main__':
    unittest.main()
