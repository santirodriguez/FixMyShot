#!/usr/bin/env python3
"""Offline collector boundaries; no request is sent and no downloaded code is executed."""
import hashlib
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
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


if __name__ == '__main__':
    unittest.main()
