#!/usr/bin/env python3
import importlib.util
import json
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location('review', Path(__file__).with_name('review-kotlin-source-checksums.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


class PublisherSourceChecksums(unittest.TestCase):
    def setUp(self):
        self.publisher = (Path(__file__).resolve().parents[1]/'docs/feasibility/evidence/kotlin-release-source-2.4.21.xml').read_bytes()
        components = ET.fromstring(self.publisher).findall('.//{*}component')
        self.owned = self.row(next(c for c in components if c.attrib == {'group':'org.jetbrains', 'name':'annotations', 'version':'23.0.0'}))
        self.foreign = self.row(next(c for c in components if c.attrib.get('group') == 'com.squareup.okio'))

    def row(self, component):
        artifact = component.find('{*}artifact')
        return {**component.attrib, 'file':artifact.attrib['name'], 'sha256':artifact.find('{*}sha256').attrib['value'], 'proof':'fixture'}

    def review(self, rows=None, publisher=None, pin=None):
        rows = rows if rows is not None else [self.owned, self.foreign]
        metadata = m.batch.core.candidate_xml(rows)
        prior = json.dumps({'status':'PARTIAL_PUBLISHER_REVIEW__NOT_ACTIVE', 'active_gradle_policy_changed':False,
                            'baseline_metadata_sha256':m.batch.digest(metadata), 'reviewed_artifacts':[],
                            'reviewed_artifact_count':0, 'captured_artifact_count':len(rows),
                            'remaining_unreviewed_artifact_count':len(rows)}).encode()
        return m.review(metadata, prior, self.publisher if publisher is None else publisher, m.batch.digest(prior) if pin is None else pin)

    def test_only_exact_owned_file_is_approved_and_foreign_checksum_is_ignored(self):
        record, _ = self.review()
        self.assertEqual(record['reviewed_artifacts'][0]['name'], 'annotations')
        self.assertEqual(record['reviewed_artifact_count'], 1)
        self.assertEqual(record['remaining_unreviewed_artifact_count'], 1)
        self.assertFalse(record['active_gradle_policy_changed'])
        self.assertFalse(record['publisher_source_checksum_reviews'][-1]['pgp_signature_claim'])

    def test_same_size_publisher_change_and_uninspected_prior_are_refused(self):
        with self.assertRaises(ValueError):self.review(publisher=b'X'+self.publisher[1:])
        with self.assertRaises(ValueError):self.review(pin='0'*64)

    def test_exact_artifact_checksum_conflict_is_refused(self):
        with self.assertRaises(ValueError):self.review(rows=[{**self.owned, 'sha256':'0'*64}])

    def test_adjacent_pom_or_module_is_not_approved_by_a_jar_hash(self):
        rows = [{**self.owned, 'file':'annotations-23.0.0.pom'}, {**self.owned, 'file':'annotations-23.0.0.module'}]
        record, _ = self.review(rows=rows)
        self.assertEqual(record['reviewed_artifact_count'], 0)


if __name__ == '__main__':unittest.main()
