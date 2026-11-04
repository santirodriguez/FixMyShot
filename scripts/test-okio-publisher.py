#!/usr/bin/env python3
"""Actual captured signatures and exact-input rejection checks; offline/public keys only."""
import importlib.util
import json
from pathlib import Path
import unittest
import zipfile

spec = importlib.util.spec_from_file_location('okio', Path(__file__).with_name('review-okio-publisher.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)
ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT/'docs/feasibility/evidence'


class OkioPublisher(unittest.TestCase):
    def setUp(self):
        record = json.loads((ROOT/'gradle/migration-f9434ab9/dependency-provenance-review.json').read_bytes())
        if 'okio_publisher_reviews' in record:
            # Later append-only reviews must not change this historical replay.
            keys = list(record)
            for key in keys[keys.index('okio_publisher_reviews'):]:
                record.pop(key)
            record['reviewed_artifacts'] = record['reviewed_artifacts'][:294]
            record['reviewed_artifact_count'] = 294
            record['remaining_unreviewed_artifact_count'] = 564
        self.args = [(EVIDENCE/'gradle-baseline-f9434ab9.xml').read_bytes(),
                     (json.dumps(record, indent=2)+'\n').encode(),
                     (EVIDENCE/'runtime-publisher-anchors.json').read_bytes(),
                     (EVIDENCE/'square-issuer-key-2021.asc').read_bytes(),
                     (EVIDENCE/'runtime-publishers-04a8aa02.zip').read_bytes()]

    def test_actual_three_signatures_and_all_prior_proofs(self):
        result, xml = m.review(*self.args)
        old = json.loads(self.args[1])
        self.assertEqual(result['reviewed_artifacts'][:-3], old['reviewed_artifacts'])
        self.assertEqual((result['reviewed_artifact_count'], result['remaining_unreviewed_artifact_count']), (297,561))
        self.assertEqual({tuple(x['identity']) for x in result['okio_publisher_reviews'][0]['outcomes']}, set(m.EXACT.values()))
        self.assertFalse(result['active_gradle_policy_changed'])
        self.assertFalse(result['okio_publisher_reviews'][0]['namespace_wide_trust'])
        self.assertIn(b'69173608417a2113e6fa6afeb7b4540b20df70cfda3fa16c73aaa4fa702ffad3', xml)

    def test_all_five_input_mutations_refused(self):
        for index in range(5):
            changed = self.args.copy()
            changed[index] += b'changed'
            with self.subTest(index=index), self.assertRaises((ValueError, m.runtime.core.ET.ParseError)):
                m.review(*changed)

    def test_actual_signature_artifact_mutation_refused(self):
        with zipfile.ZipFile(EVIDENCE/'runtime-publishers-04a8aa02.zip') as archive:
            signature, artifact = (archive.read('runtime-004'+ext) for ext in ('.asc','.bin'))
        with self.assertRaises(ValueError):
            m.runtime.verify(self.args[3], signature, artifact+b'changed', m.PRIMARY)
        with self.assertRaises(ValueError):
            m.runtime.verify(self.args[3], signature, artifact, '0'*40)

    def test_previous_stripped_key_still_refused(self):
        with zipfile.ZipFile(EVIDENCE/'runtime-publishers-04a8aa02.zip') as archive:
            with self.assertRaises(ValueError):
                m.runtime.verify(archive.read('square-key.bin'), archive.read('runtime-004.asc'),
                                 archive.read('runtime-004.bin'), m.PRIMARY)


if __name__ == '__main__':
    unittest.main()
