#!/usr/bin/env python3
"""Synthetic publisher evidence exercises exact paths and refusal boundaries."""
import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('review', Path(__file__).with_name('review-kotlin-manifests.py'))
reviewer = importlib.util.module_from_spec(spec)
spec.loader.exec_module(reviewer)


def encoded(value):
    return (json.dumps(value, indent=2)+'\n').encode()


class PublisherReview(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.sha = 'a'*64
        self.control = 'c'*40
        self.rows = [{'group': group, 'name': 'compiler', 'version': version, 'file': file, 'sha256': self.sha, 'proof': 'Synthetic capture'}
                     for group, version, file in [('org.jetbrains.kotlin', '2.2.0', 'compiler-2.2.0.jar'),
                                                  ('org.other', '2.2.0', 'compiler-2.2.0.jar'),
                                                  ('org.jetbrains.kotlin', '2.2.1', 'compiler-2.2.0.jar'),
                                                  ('org.jetbrains.kotlin', '2.2.0', 'unlisted.jar')]]
        self.metadata = reviewer.core.candidate_xml(self.rows)
        self.prior = {'status': 'PARTIAL_PUBLISHER_REVIEW__NOT_ACTIVE', 'active_gradle_policy_changed': False,
                      'baseline_source_commit': 'b'*40, 'baseline_metadata_sha256': reviewer.digest(self.metadata),
                      'reviewed_artifacts': [], 'reviewed_artifact_count': 0, 'captured_artifact_count': 4,
                      'remaining_unreviewed_artifact_count': 4, 'current_vulnerability_review': 'OPEN'}
        data = ('org/jetbrains/kotlin/compiler/2.2.0/compiler-2.2.0.jar '+self.sha+'\n').encode()
        source = {'version': '2.2.0', 'release_id': 1, 'asset_id': 2, 'size': len(data), 'sha256': reviewer.digest(data),
                  'asset_url': 'https://github.com/JetBrains/kotlin/releases/download/v2.2.0/maven-2.2.0-sha256.txt',
                  'release_url': 'https://github.com/JetBrains/kotlin/releases/tag/v2.2.0'}
        self.registry = {'schema': 1, 'publisher_repository': 'JetBrains/kotlin', 'sources': [source]}
        self.entry = {'id': 'kotlin-manifest-2-2-0', 'kind': 'publisher', 'url': source['asset_url'],
                      'sha256': source['sha256'], 'max_bytes': 262144}
        self.request = {k: self.prior[k] for k in ('baseline_source_commit', 'baseline_metadata_sha256')}
        self.request.update(schema=1, entries=[self.entry])
        self.report = {k: self.prior[k] for k in ('baseline_source_commit', 'baseline_metadata_sha256')}
        self.report.update(control_commit=self.control, scope='PUBLISHER_EVIDENCE_ONLY', automatically_authenticated=False,
                           results=[{'request': self.entry, 'status': 'MATCHED_PIN', 'authenticated': False,
                                     'sha256': source['sha256'], 'bytes': len(data)}])
        (self.root/(self.entry['id']+'.bin')).write_bytes(data)

    def run_review(self, pins=None):
        request, prior, registry = map(encoded, (self.request, self.prior, self.registry))
        (self.root/'request.json').write_bytes(request)
        (self.root/'report.json').write_bytes(encoded(self.report))
        expected = [self.control, reviewer.digest(request), reviewer.digest(prior), reviewer.digest(registry)]
        if pins:
            expected[pins[0]] = pins[1]
        return reviewer.review(self.root, self.metadata, prior, registry, *expected)

    def test_exact_path_only_and_inactive_security_open(self):
        result, xml = self.run_review()
        self.assertEqual(result['reviewed_artifact_count'], 1)
        self.assertEqual(result['reviewed_artifacts'][0]['group'], 'org.jetbrains.kotlin')
        self.assertEqual(result['remaining_unreviewed_artifact_count'], 3)
        self.assertFalse(result['active_gradle_policy_changed'])
        self.assertEqual(result['current_vulnerability_review'], 'OPEN')
        self.assertEqual(len(reviewer.core.captured_rows(xml)), 1)

    def test_source_request_prior_and_control_pins(self):
        for i in range(4):
            with self.subTest(pin=i), self.assertRaises(ValueError):
                self.run_review((i, '0'*(40 if i == 0 else 64)))

    def test_unsafe_duplicate_invalid_paths_refused(self):
        line = 'org/jetbrains/kotlin/compiler/2.2.0/compiler-2.2.0.jar '+self.sha+'\n'
        for data in (line*2, '../escape '+self.sha, '/absolute '+self.sha, 'x//y '+self.sha, 'x\\y '+self.sha, 'x nope'):
            with self.subTest(data=data), self.assertRaises(ValueError):
                reviewer.manifest(data.encode())

    def test_conflicting_exact_baseline_hash_refused(self):
        rows = copy.deepcopy(self.rows)
        rows[0]['sha256'] = 'd'*64
        self.metadata = reviewer.core.candidate_xml(rows)
        for obj in (self.prior, self.request, self.report):
            obj['baseline_metadata_sha256'] = reviewer.digest(self.metadata)
        with self.assertRaises(ValueError):
            self.run_review()

    def test_missing_capture_stays_pending(self):
        self.report['results'][0]['status'] = 'FETCH_OR_VALIDATION_ERROR'
        (self.root/(self.entry['id']+'.bin')).unlink()
        result, _ = self.run_review()
        self.assertEqual(result['reviewed_artifact_count'], 0)
        self.assertFalse(result['complete_for_baseline'])

    def test_changed_capture_or_registry_origin_refused(self):
        (self.root/(self.entry['id']+'.bin')).write_bytes(b'changed')
        with self.assertRaises(ValueError):
            self.run_review()
        self.registry['sources'][0]['asset_url'] = 'https://example.com/same-name'
        with self.assertRaises(ValueError):
            self.run_review()

    def test_existing_review_preserved_without_duplicate(self):
        self.prior.update(reviewed_artifacts=[{**self.rows[0], 'proof': 'Existing independently pinned proof'}],
                          reviewed_artifact_count=1, remaining_unreviewed_artifact_count=3)
        result, _ = self.run_review()
        self.assertEqual(result['reviewed_artifact_count'], 1)
        self.assertEqual(result['publisher_manifest_batches'][-1]['verified_file_count'], 0)
        self.assertEqual(result['reviewed_artifacts'][0]['proof'], 'Existing independently pinned proof')


if __name__ == '__main__':
    unittest.main()
