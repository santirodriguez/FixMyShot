#!/usr/bin/env python3
"""Offline Maven routing/budget boundaries; no trust or network is supplied by a namespace."""
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('planner', Path(__file__).with_name('plan-google-batch.py'))
planner = importlib.util.module_from_spec(spec)
spec.loader.exec_module(planner)


class GoogleBatchTests(unittest.TestCase):
    def fixture(self, rows):
        xml = planner.reviewer.candidate_xml([{**x, 'proof': 'UNREVIEWED test fixture'} for x in rows])
        record = {'baseline_source_commit': 'a'*40, 'baseline_metadata_sha256': hashlib.sha256(xml).hexdigest(),
                  'active_gradle_policy_changed': False, 'reviewed_artifacts': []}
        return xml, record

    def row(self, name='ui-android', filename='ui.aar', group='androidx.compose.ui'):
        return {'group': group, 'name': name, 'version': '1.10.0', 'file': filename, 'sha256': 'b'*64}

    def test_alias_preserves_baseline_identity_while_requesting_canonical_maven_bytes(self):
        row = self.row();xml, record = self.fixture([row]);policy = planner.plan(xml, record)
        item = policy['entries'][0]
        self.assertEqual('ui.aar', item['file'])
        self.assertTrue(item['url'].endswith('/ui-android/1.10.0/ui-android-1.10.0.aar'))
        self.assertEqual('b'*64, item['sha256'])
        self.assertEqual('annotation-1.10.0.jar', planner.remote_filename(self.row('annotation', 'annotation-metadata-1.10.0.jar')))

    def test_prior_approvals_and_failed_or_unsigned_attempts_are_not_automatically_retried(self):
        row = self.row();xml, record = self.fixture([row])
        record['reviewed_artifacts'] = [row];self.assertEqual([], planner.plan(xml, record)['entries'])
        record['reviewed_artifacts'] = [];record['publisher_batches'] = [{'outcomes': [{'identity': list(planner.identity(row)), 'status': 'SIGNATURE_ABSENT'}]}]
        self.assertEqual([], planner.plan(xml, record)['entries'])

    def test_namespace_routing_does_not_include_other_publishers_or_tolerate_wrong_baselines(self):
        row = self.row(group='unrelated.publisher');xml, record = self.fixture([row])
        self.assertEqual([], planner.plan(xml, record)['entries'])
        record['baseline_metadata_sha256'] = '0'*64
        with self.assertRaises(ValueError):planner.plan(xml, record)
        for filename in ['../outside.aar', 'file?query.aar']:
            with self.assertRaises(ValueError):planner.remote_filename(self.row(filename=filename))

    def test_binary_and_metadata_caps_remain_within_collector_count_and_size_budget(self):
        rows = [self.row('m'+str(i), 'm'+str(i)+'-1.10.0.jar') for i in range(60)]
        rows += [self.row('p'+str(i), 'p'+str(i)+'-1.10.0.'+ext) for ext in ('pom', 'module') for i in range(30)]
        xml,record = self.fixture(rows);policy = planner.plan(xml, record)
        self.assertEqual(96, len(policy['entries']))
        self.assertEqual(48, sum(x['file'].endswith('.jar') for x in policy['entries']))
        with tempfile.TemporaryDirectory() as d:
            p = Path(d)/'request.json';p.write_text(json.dumps(policy));planner.collector.read_policy(p)
        self.assertLess(sum(x['max_bytes']+131072 for x in policy['entries']), planner.collector.MAX_TOTAL)


if __name__ == '__main__':
    unittest.main()
