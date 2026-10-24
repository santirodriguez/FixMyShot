#!/usr/bin/env python3
"""Offline publisher-review refusal tests; actual cryptography is exercised on captured evidence."""
import hashlib
import importlib.util
from pathlib import Path
from unittest import mock
import unittest
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location('review', Path(__file__).with_name('review-dependency-provenance.py'))
review = importlib.util.module_from_spec(spec)
spec.loader.exec_module(review)


class PublisherReviewTests(unittest.TestCase):
    def test_qualified_publisher_paths_are_preserved_and_digest_pin_is_required(self):
        data = b'org/jetbrains/kotlin/module/2.2.21/file.jar ' + b'a'*64 + b'\n'
        with self.assertRaises(ValueError):
            review.publisher_manifest(data)
        with mock.patch.object(review, 'KOTLIN_MANIFEST_SHA256', hashlib.sha256(data).hexdigest()):
            self.assertEqual({'org/jetbrains/kotlin/module/2.2.21/file.jar': 'a'*64}, review.publisher_manifest(data))

    def test_duplicate_and_unsafe_manifest_paths_cannot_create_provenance(self):
        for data in [b'file.jar '+b'a'*64+b'\nfile.jar '+b'a'*64+b'\n',
                     b'../file.jar '+b'a'*64+b'\n', b'/file.jar '+b'a'*64+b'\n']:
            with mock.patch.object(review, 'KOTLIN_MANIFEST_SHA256', hashlib.sha256(data).hexdigest()), self.assertRaises(ValueError):
                review.publisher_manifest(data)

    def test_only_successful_validsig_from_exact_publisher_fingerprint_is_accepted(self):
        status = '[GNUPG:] VALIDSIG ' + review.JUNIT_FINGERPRINT + ' 2021-02-13'
        self.assertEqual(status, review.valid_signer(0, status))
        for code, text in [(1, status), (0, '[GNUPG:] GOODSIG friendly-name'),
                           (0, status.replace(review.JUNIT_FINGERPRINT, 'A'*40)),
                           (0, status+'\n[GNUPG:] REVKEYSIG revoked'), (0, status+'\n'+status)]:
            with self.subTest(code=code, text=text), self.assertRaises(ValueError):
                review.valid_signer(code, text)

    def test_wrong_publisher_key_is_refused_before_any_crypto_process(self):
        with mock.patch.object(review.subprocess, 'run') as called, self.assertRaises(ValueError):
            review.verify_junit(b'untrusted-key', b'signature', b'jar')
        called.assert_not_called()

    def test_candidate_preserves_exact_identity_and_remains_a_nonactive_checksum_subset(self):
        rows = [{'group': 'org.jetbrains.kotlin', 'name': 'module', 'version': '2.2.21',
                 'file': 'module.jar', 'sha256': 'a'*64, 'proof': 'publisher evidence'}]
        xml = review.candidate_xml(rows)
        root = ET.fromstring(xml)
        ns = {'g': review.NS}
        components = root.findall('g:components/g:component', ns)
        self.assertEqual(1, len(components))
        self.assertEqual({'group': 'org.jetbrains.kotlin', 'name': 'module', 'version': '2.2.21'}, components[0].attrib)
        self.assertEqual('a'*64, components[0].find('g:artifact/g:sha256', ns).attrib['value'])
        self.assertIn(b'PARTIAL REVIEWED CANDIDATE', xml)


if __name__ == '__main__':
    unittest.main()
