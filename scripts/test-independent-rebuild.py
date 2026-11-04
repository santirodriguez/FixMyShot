#!/usr/bin/env python3
"""Offline rebuild-control checks; Git fixtures stay in disposable temporary directories."""
import copy
import hashlib
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

spec = importlib.util.spec_from_file_location('rebuild', Path(__file__).with_name('check-independent-rebuild.py'))
rebuild = importlib.util.module_from_spec(spec)
spec.loader.exec_module(rebuild)


class RebuildTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.root = self.base / 'source'
        subprocess.run(['git', 'init', '-q', str(self.root)], check=True)
        (self.root / 'source.txt').write_text('fixture\n')
        subprocess.run(['git', '-C', str(self.root), 'add', 'source.txt'], check=True)
        subprocess.run(['git', '-C', str(self.root), '-c', 'user.name=Test',
                        '-c', 'user.email=test@example.invalid', 'commit', '-qm', 'fixture'], check=True)
        self.apk = self.base / 'candidate.apk'
        self.apk.write_bytes(b'unsigned-byte-fixture')
        self.request = {'schema': 1, 'source_commit': rebuild.git(self.root, 'rev-parse', 'HEAD'),
                        'source_tree': rebuild.git(self.root, 'rev-parse', 'HEAD^{tree}'),
                        'baseline_run_id': 1, 'baseline_artifact_id': 2,
                        'baseline_archive_sha256': 'a' * 64,
                        'expected_unsigned_apk_sha256': hashlib.sha256(self.apk.read_bytes()).hexdigest(),
                        'expected_unsigned_apk_bytes': self.apk.stat().st_size}
        self.request_path = self.base / 'request.json'
        self.request_path.write_text(json.dumps(self.request))

    def test_request_rejects_unknown_missing_malformed_boolean_and_oversized_inputs(self):
        self.assertEqual(self.request, rebuild.read_request(self.request_path))
        for key, value in [('source_commit', 'a' * 40 + '\ninjected=1'), ('source_tree', 'bad'),
                           ('schema', True), ('baseline_run_id', True), ('baseline_artifact_id', 0),
                           ('expected_unsigned_apk_bytes', 128 * 1024 * 1024 + 1), ('unexpected', 1)]:
            bad = copy.deepcopy(self.request)
            bad[key] = value
            self.request_path.write_text(json.dumps(bad))
            with self.subTest(key=key), self.assertRaises(ValueError):
                rebuild.read_request(self.request_path)
        bad = copy.deepcopy(self.request)
        del bad['source_tree']
        self.request_path.write_text(json.dumps(bad))
        with self.assertRaises(ValueError):
            rebuild.read_request(self.request_path)
        self.request_path.write_text(json.dumps(self.request)[:-1] + ', \"schema\": 1}')
        with self.assertRaises(ValueError):
            rebuild.read_request(self.request_path)
        self.request_path.write_text(' ' * 8193)
        with self.assertRaises(ValueError):
            rebuild.read_request(self.request_path)

    def test_only_exact_pristine_source_is_a_fresh_start(self):
        rebuild.verify_source(self.request, self.root, pristine=True)
        bad = dict(self.request, source_tree='0' * 40)
        with self.assertRaises(ValueError):
            rebuild.verify_source(bad, self.root, pristine=True)
        (self.root / 'untracked.txt').write_text('untracked')
        with self.assertRaises(ValueError):
            rebuild.verify_source(self.request, self.root, pristine=True)
        (self.root / 'untracked.txt').unlink()
        (self.root / 'source.txt').write_text('changed')
        with self.assertRaises(ValueError):
            rebuild.verify_source(self.request, self.root, pristine=False)

    def test_existing_cache_and_project_outputs_refuse_reuse(self):
        cache = self.base / 'gradle'
        rebuild.verify_fresh_start(self.root, cache)
        cache.mkdir()
        rebuild.verify_fresh_start(self.root, cache)
        (cache / 'old-cache').write_text('old')
        with self.assertRaises(ValueError):
            rebuild.verify_fresh_start(self.root, cache)
        (cache / 'old-cache').unlink()
        (self.root / 'app/build').mkdir(parents=True)
        with self.assertRaises(ValueError):
            rebuild.verify_fresh_start(self.root, cache)

    def test_exact_bytes_match_without_claiming_dependency_signing_or_physical_acceptance(self):
        report = rebuild.compare(self.request, self.root, self.apk, 'c' * 40)
        self.assertTrue(report['byte_identical'])
        self.assertEqual('OPEN', report['dependency_authentication'])
        self.assertEqual('NOT_RUN', report['production_signature'])
        self.assertEqual('NOT_RUN', report['physical_acceptance'])
        self.apk.write_bytes(b'changed-byte-fixture')
        self.assertFalse(rebuild.compare(self.request, self.root, self.apk, 'c' * 40)['byte_identical'])

    def test_mismatch_cli_fails_and_keeps_a_readable_comparison_report(self):
        self.apk.write_bytes(b'changed')
        report = self.base / 'report.json'
        result = subprocess.run([sys.executable, str(Path(rebuild.__file__)), 'compare',
                                 str(self.request_path), '--workspace', str(self.root), '--apk', str(self.apk),
                                 '--control-commit', 'c' * 40, '--report', str(report)],
                                capture_output=True, text=True)
        self.assertNotEqual(0, result.returncode)
        self.assertFalse(json.loads(report.read_text())['byte_identical'])

    def test_dirty_source_invalid_control_and_symlinked_output_cannot_produce_acceptance(self):
        with self.assertRaises(ValueError):
            rebuild.compare(self.request, self.root, self.apk, 'bad')
        link = self.base / 'link.apk'
        link.symlink_to(self.apk)
        with self.assertRaises(ValueError):
            rebuild.compare(self.request, self.root, link, 'c' * 40)
        (self.root / 'source.txt').write_text('changed')
        with self.assertRaises(ValueError):
            rebuild.compare(self.request, self.root, self.apk, 'c' * 40)


if __name__ == '__main__':
    unittest.main()
