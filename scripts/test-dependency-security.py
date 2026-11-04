#!/usr/bin/env python3
"""Offline refusal and scope checks; never contacts OSV."""
import copy
import importlib.util
import json
import hashlib
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
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            native = 'b' * 40
            data = {audit.BASELINE: b'<verification-metadata xmlns="https://schema.gradle.org/dependency-verification"><components><component group="example" name="runtime" version="1"/><component group="example" name="captured" version="1"/></components></verification-metadata>',
                    audit.LOCKS[0]: b'example:runtime:1=releaseRuntimeClasspath\n',
                    audit.LOCKS[1]: b'empty=\n', audit.LOCKS[2]: b'empty=\n',
                    audit.NATIVE: ('GIT_TAG '+native+'\n').encode()}
            policy = {'schema': 1, 'baseline_source_commit': 'a'*40, 'native_commit': native,
                      'inputs': {p: hashlib.sha256(b).hexdigest() for p,b in data.items()}}
            for p,b in data.items():
                target = root/p
                target.parent.mkdir(parents=True, exist_ok=True)
                target.write_bytes(b)
            rows = audit.inventory(root, policy)
            self.assertEqual(len(rows), 3)
            self.assertEqual(len({x['coordinate'] for x in rows}), len(rows))
            self.assertTrue(any(x['scope'] == 'capture_without_lockfile_selection' for x in rows))
            runtime = [x for x in rows if x['scope'] == 'app_release_runtime']
            self.assertEqual(len(runtime), 1)
            self.assertEqual(runtime[0]['coordinate'], 'example:runtime:1')
            changed = copy.deepcopy(policy)
            changed['inputs'][audit.LOCKS[0]] = '0'*64
            with self.assertRaises(ValueError):
                audit.inventory(root, changed)
            changed = copy.deepcopy(policy)
            changed['native_commit'] = '0'*40
            with self.assertRaises(ValueError):
                audit.inventory(root, changed)
            changed = copy.deepcopy(policy)
            changed['baseline_path'] = '../unowned.xml'
            with self.assertRaises(ValueError):
                audit.inventory(root, changed)
            alternate = 'docs/feasibility/evidence/gradle-baseline-1234abcd.xml'
            (root/alternate).write_bytes(data[audit.BASELINE])
            changed = copy.deepcopy(policy)
            changed['baseline_path'] = alternate
            changed['inputs'][alternate] = changed['inputs'].pop(audit.BASELINE)
            self.assertEqual(rows, audit.inventory(root, changed))

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

    def test_standalone_requires_scope_and_validated_inputs_before_network(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            policy = root/'request.json'
            original = audit.Capture
            audit.Capture = lambda *_: self.fail('invalid standalone request created a network client')
            try:
                policy.write_text('{}')
                with self.assertRaises(ValueError):
                    audit.collect(root, policy, root/'out', 'a'*40, standalone=True)
                policy.write_text(json.dumps({'collection_scope': 'STANDALONE_PINNED_SECURITY_INVENTORY'}))
                with self.assertRaises((ValueError, KeyError)):
                    audit.collect(root, policy, root/'out', 'a'*40, standalone=True)
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
