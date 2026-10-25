#!/usr/bin/env python3
"""Refuse mismatched lineage and never transfer proof across a changed identity/hash."""
import copy
import importlib.util
import json
from pathlib import Path
import unittest

spec = importlib.util.spec_from_file_location('rebase', Path(__file__).with_name('rebase-publisher-review.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


def metadata(version='1', value='a'*64):
    return ('<verification-metadata xmlns="https://schema.gradle.org/dependency-verification"><components>'
            '<component group="example" name="library" version="'+version+'"><artifact name="library.jar">'
            '<sha256 value="'+value+'"/></artifact></component></components></verification-metadata>').encode()


class Rebase(unittest.TestCase):
    def setUp(self):
        self.old = metadata()
        self.row = {**m.batch.core.captured_rows(self.old)[0], 'proof': 'anchored historical proof',
                    'signature_sha256': 'b'*64}
        self.record = {'status': 'PARTIAL_PUBLISHER_REVIEW__NOT_ACTIVE', 'active_gradle_policy_changed': False,
                       'baseline_metadata_sha256': m.batch.digest(self.old), 'reviewed_artifacts': [self.row],
                       'reviewed_artifact_count': 1, 'captured_artifact_count': 1, 'remaining_unreviewed_artifact_count': 0}

    def run_review(self, new=None, record=None, **changes):
        new = self.old if new is None else new
        raw = json.dumps(self.record if record is None else record).encode()
        kwargs = {'prior_pin': m.batch.digest(raw), 'metadata_pin': m.batch.digest(new),
                  'source': 'c'*40, 'prior_source': 'd'*40, **changes}
        return m.rebase(self.old, raw, new, **kwargs)

    def test_exact_proof_bytes_and_lineage_preserved_without_activation(self):
        before = copy.deepcopy(self.record)
        result = self.run_review()
        self.assertEqual(result['reviewed_artifacts'], [self.row])
        self.assertEqual(self.record, before)
        self.assertFalse(result['active_gradle_policy_changed'])
        self.assertEqual(result['inherited_review']['commit'], 'd'*40)
        result['reviewed_artifacts'][0]['proof'] = 'changed output'
        self.assertEqual(self.record, before)

    def test_different_version_is_not_approved(self):
        result = self.run_review(metadata(version='2'))
        self.assertEqual(result['reviewed_artifact_count'], 0)
        self.assertEqual(result['remaining_unreviewed_artifact_count'], 1)

    def test_same_identity_with_changed_bytes_refused(self):
        with self.assertRaises(ValueError):
            self.run_review(metadata(value='b'*64))

    def test_changed_pins_source_and_duplicate_prior_proof_refused(self):
        for changes in ({'prior_pin': '0'*64}, {'metadata_pin': '0'*64}, {'source': 'not-a-commit'}):
            with self.subTest(changes=changes), self.assertRaises(ValueError):
                self.run_review(**changes)
        record = copy.deepcopy(self.record)
        record['reviewed_artifacts'].append(self.row)
        with self.assertRaises(ValueError):
            self.run_review(record=record)


if __name__ == '__main__':
    unittest.main()
