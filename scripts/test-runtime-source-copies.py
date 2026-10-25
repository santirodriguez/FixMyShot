#!/usr/bin/env python3
"""Offline proof-boundary tests; no network or downloaded-code execution."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location('runtime_source', Path(__file__).with_name('review-runtime-source-copies.py'))
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


class RuntimeSource(unittest.TestCase):
    def setUp(self):
        directory = tempfile.TemporaryDirectory()
        self.addCleanup(directory.cleanup)
        self.evidence = Path(directory.name)
        self.data = b'synthetic exact runtime archive\0payload'
        self.identity = ('androidx.fixture', 'sample', '1', 'sample-1.aar')
        self.row = dict(zip(('group', 'name', 'version', 'file'), self.identity), sha256=m.batch.digest(self.data), proof='fixture')
        self.old = {'group':'androidx.old', 'name':'old', 'version':'1', 'file':'old-1.pom', 'sha256':'a'*64, 'proof':'existing independent proof'}
        self.metadata = m.batch.core.candidate_xml([self.old, self.row])
        self.lock = b'androidx.fixture:sample:1=releaseRuntimeClasspath\n'
        self.prior = {'status':'PARTIAL_PUBLISHER_REVIEW__NOT_ACTIVE', 'active_gradle_policy_changed':False,
                      'baseline_metadata_sha256':m.batch.digest(self.metadata), 'reviewed_artifacts':[self.old],
                      'reviewed_artifact_count':1, 'captured_artifact_count':2, 'remaining_unreviewed_artifact_count':1}
        self.prior_bytes = (json.dumps(self.prior)+'\n').encode()
        blob = hashlib.sha1(b'blob '+str(len(self.data)).encode()+b'\0'+self.data).hexdigest()
        self.directory = b")]}'\n"+json.dumps({'id':'f'*40, 'entries':[{'name':self.identity[-1], 'type':'blob', 'mode':0o100644, 'id':blob}]}).encode()
        self.item = {'id':'runtime-000', 'identity':list(self.identity), 'expected_sha256':self.row['sha256'],
                     'url':m.route(self.identity), 'status':'MATCHED_FROZEN_BYTES',
                     **m.checked_artifact(self.identity, self.row, self.directory, self.data)}
        self.report = {'schema':1, 'scope':m.SCOPE, 'repository':m.source.REPOSITORY, 'commit':m.source.COMMIT,
                       'tag_object':m.source.TAG_OBJECT, 'metadata_sha256':m.batch.digest(self.metadata),
                       'prior_review_sha256':m.batch.digest(self.prior_bytes), 'app_lock_sha256':m.batch.digest(self.lock),
                       'collector_sha256':m.batch.digest(Path(m.__file__).read_bytes()), 'automatic_authentication':False,
                       'results':[self.item]}
        (self.evidence/'runtime-000.directory.json').write_bytes(self.directory)
        (self.evidence/'runtime-000.bin').write_bytes(self.data)

    def review(self, **changes):
        report = json.dumps(self.report).encode()
        args = dict(metadata=self.metadata, prior_bytes=self.prior_bytes, lock=self.lock,
                    prior_pin=m.batch.digest(self.prior_bytes), lock_pin=m.batch.digest(self.lock),
                    report_bytes=report, report_pin=m.batch.digest(report), evidence=self.evidence)
        return m.review(**{**args, **changes})

    def test_exact_binary_preserves_prior_proofs_without_pgp_or_activation(self):
        before = copy.deepcopy(self.prior)
        result, _ = self.review()
        self.assertEqual(result['reviewed_artifacts'][:1], before['reviewed_artifacts'])
        self.assertEqual(result['reviewed_artifact_count'], 2)
        self.assertFalse(result['active_gradle_policy_changed'])
        self.assertFalse(result['runtime_source_copy_batches'][-1]['pgp_signature_claim'])
        self.assertEqual(self.prior, before)

    def test_source_and_directory_mutations_even_same_size_are_refused(self):
        (self.evidence/'runtime-000.bin').write_bytes(b'X'+self.data[1:])
        with self.assertRaises(ValueError):self.review()
        (self.evidence/'runtime-000.bin').write_bytes(self.data)
        (self.evidence/'runtime-000.directory.json').write_bytes(self.directory+b' ')
        with self.assertRaises(ValueError):self.review()

    def test_release_origin_producer_approval_and_exact_route_are_pinned(self):
        for key, value in [('commit','0'*40), ('repository','https://example.com'),
                           ('tag_object','a'*40), ('collector_sha256','b'*64), ('automatic_authentication',True)]:
            original = self.report[key]
            self.report[key] = value
            with self.subTest(key=key), self.assertRaises(ValueError):self.review()
            self.report[key] = original
        self.item['url'] += '/other'
        with self.assertRaises(ValueError):self.review()

    def test_explicit_input_report_pins_and_unique_membership_are_required(self):
        for key in ('prior_pin', 'lock_pin', 'report_pin'):
            with self.subTest(key=key), self.assertRaises(ValueError):self.review(**{key:'0'*64})
        directory = json.loads(self.directory.split(b'\n', 1)[1])
        directory['entries'] *= 2
        (self.evidence/'runtime-000.directory.json').write_bytes(b")]}'\n"+json.dumps(directory).encode())
        with self.assertRaises(ValueError):self.review()

    def test_other_publishers_nonruntime_and_repeated_result_are_refused(self):
        for lock in (b'org.example:other:1=releaseRuntimeClasspath\n', b'androidx.fixture:sample:1=debugCompileClasspath\n'):
            with self.subTest(lock=lock), self.assertRaises(ValueError):self.review(lock=lock, lock_pin=m.batch.digest(lock))
        self.report['results'] *= 2
        with self.assertRaises(ValueError):self.review()

    def test_unavailable_has_no_approval_and_same_release_is_not_retried(self):
        self.item['status'] = 'FILE_ABSENT_IN_EXACT_SOURCE_DIRECTORY'
        result, _ = self.review()
        self.assertEqual(result['reviewed_artifacts'], self.prior['reviewed_artifacts'])
        output = self.evidence/'must-not-exist'
        prior = json.dumps(result).encode()
        with patch.object(m.urllib.request, 'build_opener', side_effect=AssertionError('network attempted')):
            with self.assertRaises(ValueError):
                m.collect(self.metadata, prior, self.lock, m.batch.digest(prior), m.batch.digest(self.lock), output)
        self.assertFalse(output.exists())


if __name__ == '__main__':
    unittest.main()
