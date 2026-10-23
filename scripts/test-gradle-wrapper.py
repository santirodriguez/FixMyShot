#!/usr/bin/env python3
"""Offline regressions for the pre-execution wrapper boundary."""
import importlib.util
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('wrapper_guard', Path(__file__).with_name('check-gradle-wrapper.py'))
guard = importlib.util.module_from_spec(spec)
spec.loader.exec_module(guard)


class WrapperTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        for name in (*guard.FILES, 'gradle/wrapper/gradle-wrapper.properties',
                     'scripts/bootstrap-gradle.sh', 'scripts/check-gradle-wrapper.py'):
            target = self.root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / name, target)

    def tearDown(self):
        self.temp.cleanup()

    def test_committed_wrapper_validates_without_a_gradle_download(self):
        guard.check(self.root)
        result = subprocess.run(['bash', str(self.root / 'scripts/bootstrap-gradle.sh')],
                                capture_output=True, text=True, timeout=10)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn('no network or Java execution', result.stdout)
        self.assertFalse((self.root / '.gradle-bootstrap').exists())

    def test_each_modified_executable_is_rejected(self):
        for name in guard.FILES:
            with self.subTest(name=name):
                path = self.root / name
                original = path.read_bytes()
                path.write_bytes(original + b'changed')
                with self.assertRaises(ValueError):
                    guard.check(self.root)
                path.write_bytes(original)

    def test_changed_url_or_digest_and_duplicate_properties_are_rejected(self):
        path = self.root / 'gradle/wrapper/gradle-wrapper.properties'
        original = path.read_text()
        for value in (original.replace('services.gradle.org', 'example.invalid'),
                      original.replace(guard.PROPERTIES['distributionSha256Sum'], '0' * 64),
                      original + 'distributionUrl=other\n'):
            path.write_text(value)
            with self.assertRaises(ValueError):
                guard.check(self.root)

    def test_missing_wrapper_is_not_regenerated_or_downloaded(self):
        (self.root / 'gradle/wrapper/gradle-wrapper.jar').unlink()
        result = subprocess.run(['bash', str(self.root / 'scripts/bootstrap-gradle.sh')],
                                capture_output=True, text=True, timeout=10)
        self.assertNotEqual(0, result.returncode)
        self.assertFalse((self.root / 'gradle/wrapper/gradle-wrapper.jar').exists())
        self.assertFalse((self.root / '.gradle-bootstrap').exists())


if __name__ == '__main__':
    unittest.main()
