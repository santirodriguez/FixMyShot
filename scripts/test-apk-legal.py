#!/usr/bin/env python3
"""Offline package-boundary refusal tests with synthetic legal texts."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
import warnings
import zipfile

spec = importlib.util.spec_from_file_location('legal', Path(__file__).with_name('check-apk-legal.py'))
legal = importlib.util.module_from_spec(spec)
spec.loader.exec_module(legal)


class LegalAssets(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        self.apk = self.root / 'fixture.apk'
        for name in legal.REQUIRED.values():
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(b'synthetic license text\n')

    def write(self, mode='valid'):
        with zipfile.ZipFile(self.apk, 'w') as apk:
            for index, (packaged, source) in enumerate(legal.REQUIRED.items()):
                if index == 0 and mode == 'missing':
                    continue
                data = (self.root / source).read_bytes()
                apk.writestr(packaged, b'X' * len(data) if index == 0 and mode == 'changed' else data)
                if index == 0 and mode == 'duplicate':
                    with warnings.catch_warnings():
                        warnings.simplefilter('ignore', UserWarning)
                        apk.writestr(packaged, data)

    def test_complete_bytes_do_not_approve_license_review(self):
        self.write()
        result = legal.inspect(self.apk, self.root)
        self.assertEqual(len(result['files']), 8)
        self.assertFalse(result['complete_third_party_license_review'])

    def test_missing_or_duplicate_asset_refused(self):
        for mode in ('missing', 'duplicate'):
            self.write(mode)
            with self.subTest(mode=mode), self.assertRaises(ValueError):
                legal.inspect(self.apk, self.root)

    def test_same_length_altered_asset_refused(self):
        self.write('changed')
        with self.assertRaises(ValueError):
            legal.inspect(self.apk, self.root)

    def test_canonical_change_invalidates_old_package(self):
        self.write()
        (self.root / 'LICENSE').write_bytes(b'new canonical version\n')
        with self.assertRaises(ValueError):
            legal.inspect(self.apk, self.root)


if __name__ == '__main__':
    unittest.main()
