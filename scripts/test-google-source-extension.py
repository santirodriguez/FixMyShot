#!/usr/bin/env python3
"""Actual-source replay, mutation refusal and explicit publisher-scope checks."""
import copy
import importlib.util
import json
from pathlib import Path
import shutil
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('extension', Path(__file__).with_name('review-google-source-extension.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT/'docs/feasibility/evidence'


class SourceExtension(unittest.TestCase):
    def setUp(self):
        record = json.loads((ROOT/'gradle/migration-f9434ab9/dependency-provenance-review.json').read_bytes())
        if 'google_source_extension_reviews' in record:
            keys = list(record)
            for key in keys[keys.index('google_source_extension_reviews'):]:
                record.pop(key)
            record['reviewed_artifacts'] = record['reviewed_artifacts'][:297]
            record['reviewed_artifact_count'] = 297
            record['remaining_unreviewed_artifact_count'] = 561
        self.prior = (json.dumps(record, indent=2)+'\n').encode()
        self.metadata = (EVIDENCE/'gradle-baseline-f9434ab9.xml').read_bytes()
        self.archive = (EVIDENCE/'runtime-publishers-04a8aa02.zip').read_bytes()
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)/'evidence'
        shutil.copytree(EVIDENCE/'google-source-extension-13d08352', self.root)

    def review(self):
        return m.review(self.metadata, self.prior, self.root, self.archive)

    def test_actual_28_new_proofs_and_old_297_unchanged(self):
        result, xml = self.review()
        old = json.loads(self.prior)
        self.assertEqual(result['reviewed_artifacts'][:297], old['reviewed_artifacts'])
        self.assertEqual((result['reviewed_artifact_count'],result['remaining_unreviewed_artifact_count']), (325,533))
        batch = result['google_source_extension_reviews'][0]
        self.assertEqual(batch['verified_new_files'],28)
        self.assertEqual(sum(x['status']=='DIRECTORY_HTTP_404' for x in batch['outcomes']),21)
        self.assertFalse(batch['pgp_signature_claim'])
        self.assertFalse(batch['namespace_wide_trust'])
        self.assertFalse(result['active_gradle_policy_changed'])
        self.assertIn(b'e4ad7607e5c0477c6f890ef26a49cb8d1bb4dffb650bab4502afee64644e3069',xml)

    def test_changed_module_and_directory_even_same_size_refused(self):
        for path in (next(self.root.glob('*.module')), next(self.root.glob('runtime-*.directory.json'))):
            original = path.read_bytes()
            path.write_bytes(b'X'+original[1:])
            with self.subTest(path=path.name),self.assertRaises((ValueError,KeyError)):
                self.review()
            path.write_bytes(original)

    def test_all_fixed_source_report_and_archive_pins_required(self):
        for name in ('report.json','guava-ownership.json','guava.directory.json'):
            path = self.root/name
            original = path.read_bytes()
            path.write_bytes(original+b'changed')
            with self.subTest(name=name),self.assertRaises(ValueError):self.review()
            path.write_bytes(original)
        self.archive += b'changed'
        with self.assertRaises(ValueError):self.review()

    def test_prior_and_frozen_metadata_changes_refused(self):
        self.prior += b'changed'
        with self.assertRaises(ValueError):self.review()
        self.prior = self.prior[:-7]
        self.metadata += b'changed'
        with self.assertRaises((ValueError,m.batch.core.ET.ParseError)):self.review()

    def test_foreign_and_adjacent_artifacts_never_selected(self):
        rows = {}
        for group, name, ext in [('androidx.valid','one','module'),('com.android.tools','two','module'),
                                  ('org.jetbrains.kotlinx','three','module'),('com.google.guava','four','module'),
                                  ('org.jspecify','five','module'),('androidx.valid','six','jar')]:
            identity = (group,name,'1',name+'-1.'+ext)
            rows[identity] = dict(zip(('group','name','version','file'),identity))
        self.assertEqual([x[0][1] for x in m.module_plan(rows,set())],['one','two'])
        self.assertEqual(m.module_plan(rows,set(rows)),[])

    def test_newly_reported_approval_or_release_change_refused_even_with_new_report_pin(self):
        path = self.root/'report.json'
        original = json.loads(path.read_text())
        for key,value in [('commit','0'*40),('automatic_authentication',True),('scope','OTHER')]:
            changed = copy.deepcopy(original)
            changed[key] = value
            raw = (json.dumps(changed)+'\n').encode()
            path.write_bytes(raw)
            with patch.object(m,'REPORT',m.batch.digest(raw)),self.subTest(key=key),self.assertRaises(ValueError):
                self.review()


if __name__ == '__main__':
    unittest.main()
