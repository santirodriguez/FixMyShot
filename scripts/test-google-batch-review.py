#!/usr/bin/env python3
"""Offline receipt/pin/partial-capture tests; real cryptography uses actual captured data separately."""
import importlib.util
import json
from pathlib import Path
import tempfile
from unittest import mock
import unittest

spec = importlib.util.spec_from_file_location('batch', Path(__file__).with_name('review-google-batch.py'))
batch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(batch)


class BatchReviewTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory();self.addCleanup(self.tmp.cleanup);self.root = Path(self.tmp.name)
        self.data, self.key, self.sig = b'baseline bytes', b'public key fixture', b'signature fixture'
        row = {'group': 'androidx.example', 'name': 'example', 'version': '1', 'file': 'example-1.jar', 'sha256': batch.digest(self.data), 'proof': 'fixture only'}
        self.metadata = batch.core.candidate_xml([row])
        self.prior = {'schema': 1, 'status': 'PARTIAL_PUBLISHER_REVIEW__NOT_ACTIVE', 'active_gradle_policy_changed': False,
                      'baseline_source_commit': 'a'*40, 'baseline_metadata_sha256': batch.digest(self.metadata),
                      'captured_artifact_count': 1, 'reviewed_artifact_count': 0, 'remaining_unreviewed_artifact_count': 1,
                      'reviewed_artifacts': [], 'current_vulnerability_review': 'OPEN'}
        self.prior_bytes = json.dumps(self.prior).encode()
        self.request = batch.planner.plan(self.metadata, self.prior)
        self.item = self.request['entries'][0]
        (self.root/'request.json').write_text(json.dumps(self.request, indent=2)+'\n')
        self.report = {'scope': 'PUBLISHER_EVIDENCE_ONLY', 'control_commit': 'c'*40, 'automatically_authenticated': False,
                       'baseline_source_commit': self.prior['baseline_source_commit'], 'baseline_metadata_sha256': self.prior['baseline_metadata_sha256'],
                       'all_required_pins_matched': True, 'results': [{'request': self.item, 'status': 'MATCHED_PIN', 'authenticated': False,
                       'sha256': self.item['sha256'], 'bytes': len(self.data), 'signature': {'status': 'CAPTURED', 'sha256': batch.digest(self.sig)}}]}
        (self.root/(self.item['id']+'.bin')).write_bytes(self.data);(self.root/(self.item['id']+'.asc')).write_bytes(self.sig)

    def run_review(self, control='c'*40, request_pin=None, prior_pin=None):
        (self.root/'report.json').write_text(json.dumps(self.report))
        with mock.patch.object(batch.core, 'GOOGLE_KEYS_SHA256', batch.digest(self.key)):
            return batch.review(self.root, self.metadata, self.prior_bytes, self.key, control,
                                request_pin or batch.digest((self.root/'request.json').read_bytes()), prior_pin or batch.digest(self.prior_bytes))

    def test_explicit_source_pins_control_and_report_identity_are_required(self):
        for kwargs in [{'prior_pin':'0'*64}, {'request_pin':'0'*64}, {'control':'d'*40}]:
            with self.subTest(kwargs=kwargs), self.assertRaises(ValueError):self.run_review(**kwargs)
        self.report['results'] = []
        with self.assertRaises(ValueError):self.run_review()

    def test_unsigned_partial_capture_remains_pending_and_is_not_replanned(self):
        self.report['results'][0]['signature'] = {'status': 'HTTP_404'}
        result, xml = self.run_review()
        self.assertEqual(0, result['reviewed_artifact_count']);self.assertEqual(1, result['remaining_unreviewed_artifact_count'])
        self.assertEqual('SIGNATURE_ABSENT', result['publisher_batches'][0]['outcomes'][0]['status'])
        self.assertEqual([], batch.planner.plan(self.metadata, result)['entries'])
        self.assertNotIn(b'<artifact ', xml)

    def test_changed_artifact_or_detached_signature_cannot_extend_the_receipt(self):
        for suffix in ('.bin', '.asc'):
            p=self.root/(self.item['id']+suffix);original=p.read_bytes();p.write_bytes(original+b'changed')
            with self.subTest(suffix=suffix), self.assertRaises(ValueError):self.run_review()
            p.write_bytes(original)

    def test_success_extends_only_exact_file_and_keeps_policy_inactive(self):
        status='[GNUPG:] VALIDSIG '+'F'*40+' 2026-10-01 1790848472 0 4 0 1 8 00 '+batch.core.GOOGLE_PRIMARY
        with mock.patch.object(batch.core, 'verify_google', return_value=status):result, xml = self.run_review()
        self.assertEqual(1, result['reviewed_artifact_count']);self.assertFalse(result['active_gradle_policy_changed'])
        self.assertEqual('OPEN', result['current_vulnerability_review'])
        self.assertEqual('VERIFIED', result['publisher_batches'][0]['outcomes'][0]['status'])
        self.assertIn(self.item['sha256'].encode(), xml)

    def test_capture_failure_or_unverified_signature_is_reported_without_trust(self):
        self.report['all_required_pins_matched']=False;self.report['results'][0]['status']='FETCH_OR_VALIDATION_ERROR'
        result,_=self.run_review();self.assertEqual('CAPTURE_UNAVAILABLE',result['publisher_batches'][0]['outcomes'][0]['status'])
        self.report['results'][0]['status']='MATCHED_PIN'
        with mock.patch.object(batch.core, 'verify_google', side_effect=ValueError('wrong primary')):result,_=self.run_review()
        self.assertEqual(0,result['reviewed_artifact_count']);self.assertEqual('SIGNATURE_UNVERIFIED',result['publisher_batches'][0]['outcomes'][0]['status'])


if __name__ == '__main__':unittest.main()
