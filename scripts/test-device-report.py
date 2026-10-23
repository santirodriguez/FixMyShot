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
        (self.root / 'native-fixture.json').write_text(json.dumps(dict(
            schema=2, status='PROBE_COMPLETE', api=36, sizes=[[64, 64], [65, 67]],
            codec_inventory={mime: {'encoders': [], 'decoders': []}
                             for mime in ('video/hevc', 'video/av01',
                                          'image/vnd.android.heic', 'image/avif')},
            samples=[dict(format=fmt, route=route, width=w, height=h,
                          status='UNAVAILABLE', reason='ENCODE_FAILED')
                     for w, h in ((64, 64), (65, 67)) for fmt, route in
                     (('HEIC', 'NATIVE_HEIC'), ('AVIF', 'NATIVE_AVIF'))])))
        (scripts / 'check-native-probe.py').write_bytes(
            (SCRIPTS / 'check-native-probe.py').read_bytes())
        grid_spec = importlib.util.spec_from_file_location('grid_validation', SCRIPTS / 'check-native-grid.py')
        grid_module = importlib.util.module_from_spec(grid_spec)
        grid_spec.loader.exec_module(grid_module)
        grid_report = dict(
            schema=2, kind='GRID_AND_PADDING_DIAGNOSTIC', status='DIAGNOSTIC_COMPLETE', api=36,
            samples=[dict(format=fmt, width=w, height=h, source_width=sw, source_height=sh,
                          padded=(w, h) != (sw, sh), grid=grid,
                          color_profile_inspection='NOT_PERFORMED', status='UNAVAILABLE',
                          reason='INVALID_OUTPUT')
                     for w, h, sw, sh, grid in grid_module.MODES
                     for fmt in grid_module.FORMATS])
        for sample in grid_report['samples']:
            if sample['format'] == 'HEIC':
                sample['encoder_trace'] = {'status': 'NOT_CREATED'}
        (self.root / 'grid-fixture.json').write_text(json.dumps(grid_report))
        (scripts / 'check-native-grid.py').write_bytes((SCRIPTS / 'check-native-grid.py').read_bytes())
        (self.root / 'heic-layout-fixture.json').write_text(json.dumps(dict(
            schema=1, kind='HEIC_PIXEL_LAYOUT_DIAGNOSTIC', api=36,
            status='UNAVAILABLE', reason='SOURCE_OUTPUT_UNAVAILABLE')))
        (scripts / 'check-heic-layout.py').write_bytes(
            (SCRIPTS / 'check-heic-layout.py').read_bytes())
        bin_dir = self.root / 'bin'; bin_dir.mkdir()
        (bin_dir / 'git').write_text('#!/bin/sh\nprintf "test-source-sha\\n"\n')
        (bin_dir / 'adb').write_text('''#!/usr/bin/env python3
import os, sys
from pathlib import Path
args = sys.argv[1:]
if 'getconf' in args: sys.exit(127)
if 'instrument' in args:
    native = any(name in a for name in ('NativeCodecProbeTest', 'NativeGridDiagnosticTest', 'NativeHeicPixelLayoutTest') for a in args)
    thumbnail = any('ReviewThumbnailCacheTest' in a for a in args)
    private_output = any('PrivateOutputExecutorTest' in a for a in args)
    transaction = any('TransactionalOutputSaverTest' in a for a in args)
    batch = any('BatchOutputCoordinatorTest' in a for a in args)
    export_flow = any('ReviewExportFlowTest' in a for a in args)
    share_intents = any('ExportShareIntentsTest' in a for a in args)
    batch_retry = any('BatchRetryFlowTest' in a for a in args)
    import_settings = any('ImportSettingsFlowTest' in a for a in args)
    recovery = any('BatchRecoveryScannerTest' in a for a in args)
    review_render = any('ReviewRenderTest' in a for a in args)
    count = 1 if native else 3 if thumbnail else 20 if private_output else 11 if transaction else 7 if batch else 5 if export_flow else 8 if batch_retry else 4 if share_intents else 16 if recovery else 21 if import_settings else 6 if review_render else 4
    print(f'INSTRUMENTATION_RESULT: stream=\\n\\nOK ({count} test' +
          ('s' if count != 1 else '') + ')\\n\\nINSTRUMENTATION_CODE: -1')
elif 'cat' in args:
    name = args[-1]
    if name.endswith('g1-native-probe.json'):
        print(Path('native-fixture.json').read_text())
    elif name.endswith('g1-grid-diagnostic.json'):
        print(Path('grid-fixture.json').read_text())
    elif name.endswith('g1-heic-pixel-layout.json'):
        print(Path('heic-layout-fixture.json').read_text())
    elif name.endswith('g1-smoke.json'):
        print('run-as: unknown package' if os.environ.get('BAD_REPORT') else Path('fixture.json').read_text())
    elif 'g1-alpha-' in name:
        if not os.environ.get('ALPHA_EVIDENCE'):
            print('run-as: missing optional fixture'); sys.exit(0)
        elif name.endswith('g1-alpha-edge.json'):
            print('{"schema":1,"kind":"SYNTHETIC_ALPHA_FAILURE","status":"CAPTURED","side":96}')
        elif name.endswith('g1-alpha-rendered.png'):
            sys.stdout.buffer.write(b'\\x89PNG\\r\\n\\x1a\\n')
        elif name.endswith('g1-alpha-candidate.webp'):
            sys.stdout.buffer.write(b'RIFF')
        else: sys.exit(1)
    elif name.endswith(('.heic', '.avif')):
        if os.environ.get('ADB_FALSE_SUCCESS'):
            print(f'cat: {name}: No such file or directory')
        else:
            sys.exit(1)
    else:
        sys.stdout.buffer.write(b'RIFF' if name.endswith('.webp') else b'\\x89PNG\\r\\n\\x1a\\n')
''')
        for p in bin_dir.iterdir(): p.chmod(0o755)
        env = dict(os.environ, PATH=str(bin_dir) + os.pathsep + os.environ['PATH'])
        for bad, false_success, alpha in ((False, True, False), (True, True, False), (False, True, True)):
            current = dict(env)
            if bad: current['BAD_REPORT'] = '1'
            if false_success: current['ADB_FALSE_SUCCESS'] = '1'
            if alpha: current['ALPHA_EVIDENCE'] = '1'
            # Use a file, not inherited pipes: timeout's adb child may outlive bash briefly.
            with tempfile.TemporaryFile(mode='w+t') as output:
                result = subprocess.run(['bash', str(SCRIPTS / 'run-device-smoke.sh')], cwd=self.root,
                    env=current, stdout=output, stderr=subprocess.STDOUT, text=True, timeout=90)
                output.seek(0)
                self.assertEqual(bad, result.returncode != 0, output.read())
            for size in ('64x64', '65x67'):
                self.assertFalse((self.root / f'evidence/g1-native-{size}.heic').exists())
                self.assertFalse((self.root / f'evidence/g1-native-{size}.avif').exists())
            for name in ('g1-alpha-edge.json', 'g1-alpha-candidate.webp', 'g1-alpha-rendered.png'):
                self.assertEqual(alpha, (self.root / 'evidence' / name).exists(), name)
            self.assertFalse((self.root / 'evidence/g1-alpha-candidate.png').exists())
            for fmt in ('heic', 'avif'):
                for variant in ('64x64-off', '65x67-off', '66x68-off', '66x68-on'):
                    self.assertFalse((self.root / f'evidence/g1-grid-{fmt}-{variant}.{fmt}').exists())
        self.assertIn('UNAVAILABLE', (self.root / 'evidence/page-size-shell.txt').read_text())


if __name__ == '__main__':
    unittest.main()


