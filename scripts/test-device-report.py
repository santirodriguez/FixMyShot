#!/usr/bin/env python3
"""Host-side evidence/collector regressions; these do not execute Android codecs."""
import copy
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPTS = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('device_report', SCRIPTS / 'check-device-report.py')
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
LOG = 'INSTRUMENTATION_RESULT: stream=\n\nOK (4 tests)\n\nINSTRUMENTATION_CODE: -1\n'


def fixture_report(api=36):
    samples = []
    for fixture in range(20):
        width, height = (1344, 2992) if fixture == 0 else (127 + fixture * 7, 241 + fixture * 11)
        for fmt in ('PNG', 'WEBP_LOSSLESS'):
            for iteration in range(2):
                samples.append(dict(fixture=fixture, format=fmt, iteration=iteration,
                    width=width, height=height, pixels='PASS', bytes=128, elapsed_ns=1000,
                    route='ANDROID_PNG' if fmt == 'PNG' else ('ANDROID_WEBP_LOSSLESS' if api >= 30 else 'LIBWEBP')))
    return dict(schema=2, status='PASS_SYNTHETIC_SMOKE_ONLY', api=api,
                page_size_bytes=4096, process_heap_limit_bytes=268435456, samples=samples)


class EvidenceTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.report = fixture_report()
        (self.root / 'instrumentation.txt').write_text(LOG)
        (self.root / 'g1-fixture-0.png').write_bytes(b'\x89PNG\r\n\x1a\n')
        (self.root / 'g1-fixture-0.webp').write_bytes(b'RIFF')

    def check(self):
        (self.root / 'g1-smoke.json').write_text(json.dumps(self.report))
        return module.validate(self.root)

    def test_complete_matrix(self):
        self.assertEqual(80, len(self.check()['samples']))

    def test_old_api_requires_native_route(self):
        self.report = fixture_report(26)
        self.check()
        self.report['samples'][2]['route'] = 'ANDROID_WEBP_LOSSLESS'
        with self.assertRaises(ValueError): self.check()

    def test_incomplete_status(self):
        self.report['status'] = 'INCOMPLETE'
        with self.assertRaises(ValueError): self.check()

    def test_missing_and_duplicate_samples(self):
        self.report['samples'].pop()
        with self.assertRaises(ValueError): self.check()
        self.report['samples'].append(copy.deepcopy(self.report['samples'][0]))
        with self.assertRaises(ValueError): self.check()

    def test_wrong_dimensions_or_failed_pixels(self):
        self.report['samples'][0]['width'] = 1334
        with self.assertRaises(ValueError): self.check()
        self.report = fixture_report()
        self.report['samples'][0]['pixels'] = 'FAIL'
        with self.assertRaises(ValueError): self.check()

    def test_missing_or_invalid_page_measurement(self):
        for value in (0, -1, 6000, True):
            self.report['page_size_bytes'] = value
            with self.assertRaises(ValueError): self.check()

    def test_failed_empty_and_crashed_instrumentation(self):
        for log in ('', 'OK (0 tests)\nINSTRUMENTATION_CODE: -1\n', LOG + 'FAILURES!!!', 'Process crashed'):
            (self.root / 'instrumentation.txt').write_text(log)
            with self.assertRaises(ValueError): self.check()

    def test_error_text_is_not_json(self):
        (self.root / 'g1-smoke.json').write_text('run-as: unknown package')
        with self.assertRaises(ValueError): module.validate(self.root)

    def test_missing_synthetic_bytes(self):
        (self.root / 'g1-fixture-0.webp').unlink()
        with self.assertRaises(OSError): self.check()

    def test_collector_handles_missing_getconf_without_masking_bad_reports(self):
        scripts = self.root / 'scripts'; scripts.mkdir()
        (scripts / 'check-device-report.py').write_bytes((SCRIPTS / 'check-device-report.py').read_bytes())
        (self.root / 'fixture.json').write_text(json.dumps(self.report))
        bin_dir = self.root / 'bin'; bin_dir.mkdir()
        (bin_dir / 'git').write_text('#!/bin/sh\nprintf "test-source-sha\\n"\n')
        (bin_dir / 'adb').write_text('''#!/usr/bin/env python3
import os, sys
from pathlib import Path
args = sys.argv[1:]
if 'getconf' in args: sys.exit(127)
if 'instrument' in args:
    print('INSTRUMENTATION_RESULT: stream=\\n\\nOK (4 tests)\\n\\nINSTRUMENTATION_CODE: -1')
elif 'cat' in args:
    name = args[-1]
    if name.endswith('.json'):
        print('run-as: unknown package' if os.environ.get('BAD_REPORT') else Path('fixture.json').read_text())
    else:
        sys.stdout.buffer.write(b'RIFF' if name.endswith('.webp') else b'\\x89PNG\\r\\n\\x1a\\n')
''')
        for p in bin_dir.iterdir(): p.chmod(0o755)
        env = dict(os.environ, PATH=str(bin_dir) + os.pathsep + os.environ['PATH'])
        for bad in (False, True):
            current = dict(env)
            if bad: current['BAD_REPORT'] = '1'
            result = subprocess.run(['bash', str(SCRIPTS / 'run-device-smoke.sh')], cwd=self.root,
                env=current, capture_output=True, text=True, timeout=10)
            self.assertEqual(bad, result.returncode != 0, result.stdout + result.stderr)
        self.assertIn('UNAVAILABLE', (self.root / 'evidence/page-size-shell.txt').read_text())


if __name__ == '__main__':
    unittest.main()
