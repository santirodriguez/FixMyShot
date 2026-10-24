#!/usr/bin/env python3
"""Offline collector boundaries; no request is sent and no downloaded code is executed."""
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest import mock
from urllib.error import HTTPError

spec = importlib.util.spec_from_file_location('collector', Path(__file__).with_name('collect-dependency-provenance.py'))
collector = importlib.util.module_from_spec(spec)
spec.loader.exec_module(collector)


class CollectorTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.data = b'public-artifact-fixture'
        self.url = 'https://repo.maven.apache.org/maven2/example/item/1/item-1.jar'
        self.policy = {'schema': 1, 'baseline_source_commit': 'a'*40, 'baseline_metadata_sha256': 'b'*64,
                       'entries': [{'id': 'fixture', 'kind': 'artifact', 'coordinate': 'example:item:1',
                                    'url': self.url, 'sha256': hashlib.sha256(self.data).hexdigest(), 'max_bytes': 1024}]}

    def load(self, policy):
        path = self.root / 'request.json'
        path.write_text(json.dumps(policy))
        return collector.read_policy(path)

    def getter(self, url, limit, deadline):
        if url == self.url:
            return self.data
        if url == self.url + '.sha256':
            return hashlib.sha256(self.data).hexdigest().encode()
        if url == self.url + '.asc':
            return b'fixture signature is NOT cryptographic proof'
        raise HTTPError(url, 404, 'absent', {}, None)

    def test_exact_bytes_and_published_hash_never_automatically_authenticate(self):
        report = collector.collect(self.load(self.policy), self.root/'out', 'c'*40, self.getter)
        self.assertTrue(report['all_required_pins_matched'])
        self.assertFalse(report['automatically_authenticated'])
        self.assertFalse(report['results'][0]['authenticated'])
        self.assertEqual('MATCHED', report['results'][0]['published_checksum']['status'])
        self.assertFalse(report['results'][0]['signature']['cryptographically_verified'])
        self.assertEqual(self.data, (self.root/'out/fixture.bin').read_bytes())

    def test_wrong_capture_hash_fails_and_retains_truthful_report(self):
        policy = json.loads(json.dumps(self.policy))
        policy['entries'][0]['sha256'] = '0'*64
        report = collector.collect(self.load(policy), self.root/'out', 'c'*40, self.getter)
        self.assertFalse(report['all_required_pins_matched'])
        self.assertEqual('SHA256_MISMATCH', report['results'][0]['status'])
        self.assertTrue((self.root/'out/report.json').is_file())

    def test_published_checksum_mismatch_is_not_silently_ignored(self):
        def bad(url, limit, deadline):
            return b'0'*64 if url.endswith('.sha256') else self.getter(url, limit, deadline)
        report = collector.collect(self.load(self.policy), self.root/'out', 'c'*40, bad)
        self.assertFalse(report['all_required_pins_matched'])
        self.assertEqual('MISMATCH', report['results'][0]['published_checksum']['status'])

    def test_optional_absence_is_recorded_without_claiming_a_signature(self):
        def missing(url, limit, deadline):
            if url != self.url:
                raise HTTPError(url, 404, 'absent', {}, None)
            return self.data
        report = collector.collect(self.load(self.policy), self.root/'out', 'c'*40, missing)
        self.assertTrue(report['all_required_pins_matched'])
        self.assertEqual('HTTP_404', report['results'][0]['signature']['status'])
        self.assertFalse(report['results'][0]['authenticated'])

    def test_policy_and_redirects_reject_unsafe_origins_paths_sizes_and_duplicates(self):
        for key, value in [('id', '../outside'), ('url', 'http://repo.maven.apache.org/file'),
                           ('url', 'https://untrusted.invalid/file'), ('sha256', 'bad'), ('max_bytes', True)]:
            policy = json.loads(json.dumps(self.policy))
            policy['entries'][0][key] = value
            with self.subTest(key=key), self.assertRaises(ValueError):
                self.load(policy)
        policy = json.loads(json.dumps(self.policy))
        policy['entries'].append(dict(policy['entries'][0]))
        with self.assertRaises(ValueError):
            self.load(policy)
        for url in ['https://untrusted.invalid/signed', 'file:///outside', 'https://user:password@github.com/file']:
            with self.subTest(url=url), self.assertRaises(ValueError):
                collector.valid_url(url, redirect=True)
        collector.valid_url('https://release-assets.githubusercontent.com/file?temporary=1', redirect=True)

    def test_oversized_response_and_existing_output_cannot_produce_acceptance(self):
        report = collector.collect(self.load(self.policy), self.root/'out', 'c'*40,
                                   lambda *args: b'x'*1025)
        self.assertFalse(report['all_required_pins_matched'])
        self.assertEqual('FETCH_OR_VALIDATION_ERROR', report['results'][0]['status'])
        with self.assertRaises(FileExistsError):
            collector.collect(self.policy, self.root/'out', 'c'*40, self.getter)


    def test_primary_key_pin_cannot_be_satisfied_by_a_subkey_or_secret_key(self):
        listing = 'pub:::::::::\nfpr:::::::::'+'A'*40+':\nsub:::::::::\nfpr:::::::::'+'B'*40+':\n'
        self.assertEqual(['A'*40], collector.primary_fingerprints(listing))
        for text in ['sec:::::::::\nfpr:::::::::'+'A'*40+':\n', listing+listing, 'pub:::::::::\nfpr:::::::::bad:\n']:
            with self.subTest(text=text), self.assertRaises(ValueError):
                collector.primary_fingerprints(text)

    def test_fingerprint_capture_is_explicitly_unauthenticated_and_mismatches_fail(self):
        policy = json.loads(json.dumps(self.policy))
        entry = policy['entries'][0]
        entry.pop('coordinate'); entry.pop('sha256')
        entry.update(kind='publisher-key', primary_fingerprint='A'*40)
        for number, fingerprints, passed in [(0, ['A'*40], True), (1, ['B'*40], False)]:
            with mock.patch.object(collector, 'public_key_identity', return_value=fingerprints):
                report = collector.collect(self.load(policy), self.root/str(number), 'c'*40, self.getter)
            self.assertEqual(passed, report['all_required_pins_matched'])
            self.assertFalse(report['automatically_authenticated'])
            self.assertFalse(report['results'][0]['authenticated'])
            self.assertFalse(report['results'][0]['cryptographically_verified'])
            self.assertEqual(hashlib.sha256(self.data).hexdigest(), report['results'][0]['sha256'])
        for value in ['short', 'a'*40, None]:
            entry['primary_fingerprint'] = value
            with self.assertRaises(ValueError):
                self.load(policy)
        entry['primary_fingerprint'] = 'A'*40; entry['max_bytes'] = 128*1024+1
        with self.assertRaises(ValueError):
            self.load(policy)

    def test_failed_public_key_parser_retains_partial_report_and_never_approves(self):
        policy = json.loads(json.dumps(self.policy))
        entry = policy['entries'][0]; entry.pop('coordinate'); entry.pop('sha256')
        entry.update(kind='publisher-key', primary_fingerprint='A'*40)
        with mock.patch.object(collector, 'public_key_identity', side_effect=ValueError('invalid key')):
            report = collector.collect(self.load(policy), self.root/'failed-key', 'c'*40, self.getter)
        self.assertFalse(report['all_required_pins_matched'])
        self.assertEqual('FETCH_OR_VALIDATION_ERROR', report['results'][0]['status'])
        self.assertTrue((self.root/'failed-key/report.json').exists())


if __name__ == '__main__':
    unittest.main()
