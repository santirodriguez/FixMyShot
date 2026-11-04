#!/usr/bin/env python3
"""Verify exact-NDK precedence without network or Android SDK downloads."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).with_name('probe-jxl-reference.sh')


class NDKSelectionTests(unittest.TestCase):
    def run_script(self, folder: Path, env_extra=None):
        env = os.environ.copy()
        env.update(env_extra or {})
        return subprocess.run(['bash', str(SCRIPT), str(folder)], env=env,
                              capture_output=True, text=True, timeout=6)

    def test_exact_ndk_absent_is_typed_not_run(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            r = self.run_script(root / 'report', {'ANDROID_HOME': str(root), 'ANDROID_NDK_HOME': '', 'ANDROID_NDK_ROOT': ''})
            self.assertEqual(2, r.returncode)
            self.assertIn('NOT_RUN: exact NDK', r.stderr)

    def test_bad_abi_rejected_before_filesystem_work(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            r = self.run_script(root / 'report', {'JXL_PROBE_ABI': 'x86_64/../../other'})
            self.assertEqual(2, r.returncode)
            self.assertIn('Invalid JXL_PROBE_ABI', r.stderr)

    def test_r28_sdk_preferred_over_stale_runner_ndk_variable(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            ndk = root / 'ndk' / '28.2.13676358'
            (ndk / 'build/cmake').mkdir(parents=True)
            (ndk / 'build/cmake/android.toolchain.cmake').write_text('stub')
            (ndk / 'source.properties').write_text('Pkg.Revision = 28.2.13676358\n')
            # Clone is stopped immediately, proving we reached past pinned NDK guard.
            binpath = root / 'tools'
            binpath.mkdir()
            git = binpath / 'git'
            git.write_text('#!/bin/sh\nexit 42\n')
            git.chmod(0o755)
            r = self.run_script(root / 'report', {'ANDROID_HOME': str(root), 'ANDROID_NDK_HOME': '/old/default/ndk',
                                                'ANDROID_NDK_ROOT': '/also/old', 'PATH': str(binpath) + os.pathsep + os.environ['PATH'],
                                                'JXL_PROBE_ABI': 'arm64-v8a'})
            self.assertEqual(42, r.returncode, r.stderr)
            self.assertNotIn('NOT_RUN: exact NDK', r.stderr)
            params = (root / 'report/evidence/parameters.txt').read_text()
            self.assertIn('ndk_revision=28.2.13676358', params)

    def test_wrong_ndk_revision_is_rejected(self):
        with tempfile.TemporaryDirectory() as d:
            root = Path(d)
            ndk = root / 'ndk' / '28.2.13676358'
            (ndk / 'build/cmake').mkdir(parents=True)
            (ndk / 'build/cmake/android.toolchain.cmake').write_text('stub')
            (ndk / 'source.properties').write_text('Pkg.Revision = 27.0.12077973\n')
            r = self.run_script(root / 'report', {'ANDROID_HOME': str(root), 'ANDROID_NDK_HOME': '', 'ANDROID_NDK_ROOT': ''})
            self.assertEqual(2, r.returncode)
            self.assertIn('NOT_RUN: exact NDK', r.stderr)


if __name__ == '__main__':
    unittest.main()
