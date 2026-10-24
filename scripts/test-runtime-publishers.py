#!/usr/bin/env python3
"""Offline source-pin, runtime-scope and exact-file refusal checks."""
import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest import mock

spec = importlib.util.spec_from_file_location('reviewer', Path(__file__).with_name('review-runtime-publishers.py'))
r = importlib.util.module_from_spec(spec)
spec.loader.exec_module(r)


def encoded(value):
    return (json.dumps(value, indent=2)+'\n').encode()


class RuntimeReview(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.primary = '2FBA29D08D2E25EE84C132C30729A0AFF8999A87'
        self.data, self.key, self.sig = b'artifact fixture', b'public key fixture', b'signature fixture'
        self.rows = [{'group': 'org.jetbrains.kotlin', 'name': name, 'version': '1', 'file': name+'-1.jar',
                      'sha256': r.digest(self.data), 'proof': 'Original exact proof'} for name in ('prior', 'runtime')]
        self.metadata = r.core.candidate_xml(self.rows)
        self.prior = {'status': 'PARTIAL_PUBLISHER_REVIEW__NOT_ACTIVE', 'active_gradle_policy_changed': False,
                      'baseline_source_commit': 'b'*40, 'baseline_metadata_sha256': r.digest(self.metadata),
                      'reviewed_artifacts': self.rows[:1], 'reviewed_artifact_count': 1, 'captured_artifact_count': 2,
                      'remaining_unreviewed_artifact_count': 1, 'current_vulnerability_review': 'OPEN'}
        source = 'Published primary '+self.primary+'\n'
        blob = r.hashlib.sha1(b'blob '+str(len(source)).encode()+b'\0'+source.encode()).hexdigest()
        self.registry = {k: self.prior[k] for k in ('baseline_source_commit', 'baseline_metadata_sha256')}
        self.registry.update(schema=1, review_scope='EXACT_BASELINE_RUNTIME_PRIORITY_ONLY', publishers=[{
            'id': 'fixture', 'key_entry': 'kotlin-key', 'primary_fingerprint': self.primary, 'groups': ['org.jetbrains.kotlin'],
            'source_commit': 'a'*40, 'source_blob': blob, 'source_text': source}])
        self.lock = b'org.jetbrains.kotlin:runtime:1=releaseRuntimeClasspath\n'
        self.item = {'id': 'runtime-000', 'kind': 'signed-artifact', 'coordinate': 'org.jetbrains.kotlin:runtime:1',
                     'file': 'runtime-1.jar', 'sha256': r.digest(self.data), 'max_bytes': 1024,
                     'url': 'https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/runtime/1/runtime-1.jar'}
        self.key_item = {'id': 'kotlin-key', 'kind': 'publisher-key', 'primary_fingerprint': self.primary,
                         'url': 'https://keys.openpgp.org/vks/v1/by-fingerprint/'+self.primary, 'max_bytes': 131072}
        self.request = {k: self.prior[k] for k in ('baseline_source_commit', 'baseline_metadata_sha256')}
        self.request.update(schema=1, entries=[self.item, self.key_item])
        self.report = {k: self.prior[k] for k in ('baseline_source_commit', 'baseline_metadata_sha256')}
        self.report.update(control_commit='c'*40, scope='PUBLISHER_EVIDENCE_ONLY', automatically_authenticated=False,
            results=[{'request': self.item, 'status': 'MATCHED_PIN', 'authenticated': False,
                      'sha256': r.digest(self.data), 'bytes': len(self.data),
                      'signature': {'status': 'CAPTURED', 'sha256': r.digest(self.sig)}},
                     {'request': self.key_item, 'status': 'MATCHED_PRIMARY_FINGERPRINT', 'authenticated': False,
                      'sha256': r.digest(self.key), 'bytes': len(self.key)}])
        for filename, data in [('runtime-000.bin', self.data), ('runtime-000.asc', self.sig), ('kotlin-key.bin', self.key)]:
            (self.root/filename).write_bytes(data)

    def run_review(self, bad_pin=None):
        prior, registry, request = map(encoded, (self.prior, self.registry, self.request))
        (self.root/'request.json').write_bytes(request)
        (self.root/'report.json').write_bytes(encoded(self.report))
        pins = ['c'*40, r.digest(request), r.digest(prior), r.digest(registry), r.digest(self.lock)]
        if bad_pin is not None:
            pins[bad_pin] = '0'*(40 if bad_pin == 0 else 64)
        with mock.patch.object(r.batch.planner.collector, 'public_key_identity', return_value=[self.primary]):
            return r.review(self.root, self.metadata, prior, registry, self.lock, *pins)

    def test_exact_success_preserves_prior_proof_and_inactive_security(self):
        status = '[GNUPG:] VALIDSIG '+'F'*40+' 2026-01-01 1767225600 0 4 0 1 10 00 '+self.primary
        with mock.patch.object(r, 'verify', return_value=status):
            result, xml = self.run_review()
        self.assertEqual(result['reviewed_artifact_count'], 2)
        self.assertEqual(result['reviewed_artifacts'][0], self.rows[0])
        self.assertFalse(result['active_gradle_policy_changed'])
        self.assertEqual(result['current_vulnerability_review'], 'OPEN')
        self.assertIn(self.item['sha256'].encode(), xml)

    def test_every_independent_pin_is_required(self):
        for number in range(5):
            with self.subTest(pin=number), self.assertRaises(ValueError):
                self.run_review(number)

    def test_changed_artifact_signature_or_key_cannot_approve(self):
        for filename in ('runtime-000.bin', 'runtime-000.asc', 'kotlin-key.bin'):
            p = self.root/filename
            original = p.read_bytes()
            p.write_bytes(original+b'changed')
            with self.subTest(filename=filename), self.assertRaises(ValueError):
                self.run_review()
            p.write_bytes(original)

    def test_missing_reordered_or_autoapproved_capture_refused(self):
        original = copy.deepcopy(self.report)
        for mutate in (lambda x: x['results'].pop(), lambda x: x['results'].reverse(),
                       lambda x: x['results'][0].update(authenticated=True),
                       lambda x: x.update(automatically_authenticated=True)):
            self.report = copy.deepcopy(original)
            mutate(self.report)
            with self.assertRaises(ValueError):
                self.run_review()

    def test_absent_anchor_or_invalid_signature_stays_pending(self):
        for mode in ('anchor', 'signature'):
            original = copy.deepcopy(self.registry)
            if mode == 'anchor':
                self.registry['publishers'][0]['groups'] = ['org.other']
            with mock.patch.object(r, 'verify', side_effect=ValueError('unverifiable')):
                result, _ = self.run_review()
            self.assertEqual(result['reviewed_artifact_count'], 1)
            self.assertEqual(result['remaining_unreviewed_artifact_count'], 1)
            self.registry = original

    def test_unavailable_capture_or_missing_signature_never_approves(self):
        capture = self.report['results'][0]
        for mode in ('capture', 'signature'):
            capture['status'] = 'HTTP_404' if mode == 'capture' else 'MATCHED_PIN'
            capture['signature']['status'] = 'HTTP_404'
            result, _ = self.run_review()
            self.assertEqual(result['reviewed_artifact_count'], 1)
            self.assertFalse(result['complete_for_baseline'])

    def test_wrong_runtime_scope_or_anchor_source_refused(self):
        original = self.lock
        self.lock = b'org.other:runtime:1=releaseRuntimeClasspath\n'
        with self.assertRaises(ValueError):
            self.run_review()
        self.lock = original
        self.registry['publishers'][0]['source_text'] += 'changed'
        with self.assertRaises(ValueError):
            self.run_review()

    def test_bad_weak_wrong_primary_or_ambiguous_status_refused(self):
        status = '[GNUPG:] VALIDSIG '+'F'*40+' 2026-01-01 1767225600 0 4 0 1 10 00 '+self.primary
        for code, text in ((1, status), (0, status+'\n[GNUPG:] BADSIG'), (0, status.replace(' 10 ', ' 2 ')),
                           (0, status[:-40]+'A'*40), (0, status+'\n'+status)):
            with self.subTest(status=text), self.assertRaises(ValueError):
                r.validate_status(code, text, '', self.primary)


if __name__ == '__main__':
    unittest.main()
