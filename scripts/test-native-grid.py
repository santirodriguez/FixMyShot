#!/usr/bin/env python3
"""Host-only synthetic regressions for the bounded grid diagnostic collector."""
import importlib.util
import json
from pathlib import Path
import struct
import tempfile
import unittest

p = Path(__file__).with_name('check-native-grid.py')
spec = importlib.util.spec_from_file_location('grid_checker', p)
checker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checker)
SUCCESS_LOG = 'INSTRUMENTATION_RESULT: stream=\n\nOK (1 test)\n\nINSTRUMENTATION_CODE: -1\n'


def report(api=36):
    data = dict(schema=2, kind='GRID_AND_PADDING_DIAGNOSTIC',
                status='DIAGNOSTIC_COMPLETE', api=api,
                samples=[dict(format=fmt, width=w, height=h, source_width=sw, source_height=sh,
                              padded=(w, h) != (sw, sh), grid=grid,
                              color_profile_inspection='NOT_PERFORMED', status='UNAVAILABLE',
                              reason='UNSUPPORTED_API' if api < min_api else 'INVALID_OUTPUT')
                         for w, h, sw, sh, grid in checker.MODES
                         for fmt, (min_api, _) in checker.FORMATS.items()])
    if api >= 29:
        for sample in data["samples"]:
            if sample["format"] == "HEIC":
                sample["encoder_trace"] = {"status": "NOT_CREATED"}
    return data


class ReportTests(unittest.TestCase):
    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.root = Path(tmp.name)
        (self.root / 'grid-instrumentation.txt').write_text(SUCCESS_LOG)
        self.data = report()

    def check(self):
        (self.root / 'g1-grid-diagnostic.json').write_text(json.dumps(self.data))
        return checker.validate(self.root)

    def test_all_unavailable_cases_count_as_truthful_collection_only(self):
        self.assertEqual(len(checker.MODES) * len(checker.FORMATS), len(self.check()['samples']))

    def test_under_api_requires_hard_rejection(self):
        self.data = report(26)
        self.check()
        self.data['samples'][0]['reason'] = 'INVALID_OUTPUT'
        with self.assertRaises(ValueError): self.check()

    def test_missing_duplicate_and_padding_falsification(self):
        self.data['samples'].pop()
        with self.assertRaises(ValueError): self.check()
        self.data = report()
        self.data['samples'][0] = self.data['samples'][2].copy()
        with self.assertRaises(ValueError): self.check()
        self.data = report()
        self.data['samples'][4]['padded'] = False
        with self.assertRaises(ValueError): self.check()

    def test_success_requires_matching_native_file_and_exact_visible_dimensions(self):
        sample = self.data['samples'][0]
        sample.update(status='ENCODED_AND_REOPENED', bytes=20, elapsed_ns=5,
                      decoded_width=64, decoded_height=64, decoded_srgb=True,
                      decoded_row_bytes=256, decoded_bitmap_config='ARGB_8888',
                      psnr_rgb_db=25.4, median_abs_rgb=[2, 1, 1])
        sample['encoder_trace'] = dict(status='CAPTURED', source='ANDROIDX_TEST_REFLECTION',
                                      codec_name='c2.android.hevc.encoder',
                                      configured_mime='video/hevc',
                                      configured_width=64, configured_height=64,
                                      requested_width=64, requested_height=64,
                                      effective_grid=False, pre_stop_output_status='NOT_READY')
        file = self.root / checker.expected_filename('HEIC', 64, 64, False)
        file.write_bytes(struct.pack('>I', 20) + b'ftypheic' + struct.pack('>I', 0) + b'mif1')
        self.check()
        sample['decoded_width'] = 65
        with self.assertRaises(ValueError): self.check()
        sample['decoded_width'] = 64
        file.write_text('cat: no such file')
        with self.assertRaises(ValueError): self.check()

    def test_missing_and_fabricated_codec_trace_rejected(self):
        sample = self.data['samples'][0]
        sample.pop('encoder_trace')
        with self.assertRaises(ValueError): self.check()
        sample['encoder_trace'] = dict(status='UNAVAILABLE', reason='REFLECTION_UNAVAILABLE')
        self.check()
        sample['encoder_trace'] = dict(status='CAPTURED', source='ANDROIDX_TEST_REFLECTION',
                                      codec_name='c2.android.hevc.encoder',
                                      configured_mime='video/hevc',
                                      configured_width=96, configured_height=72,
                                      requested_width=65, requested_height=64,
                                      effective_grid=False, pre_stop_output_status='NOT_READY')
        with self.assertRaises(ValueError): self.check()

    def test_unobserved_pre_stop_format_cannot_be_marked_available(self):
        sample = self.data['samples'][0]
        sample['encoder_trace'] = dict(status='CAPTURED', source='ANDROIDX_TEST_REFLECTION',
                                      codec_name='c2.android.hevc.encoder',
                                      configured_mime='video/hevc',
                                      configured_width=96, configured_height=72,
                                      requested_width=64, requested_height=64,
                                      effective_grid=False, pre_stop_output_status='AVAILABLE')
        with self.assertRaises(ValueError): self.check()

    def test_failing_case_may_not_retain_output(self):
        (self.root / checker.expected_filename('AVIF', 65, 67, False)).write_text('malformed')
        with self.assertRaises(ValueError): self.check()

    def test_unverified_color_tag_is_not_presented_as_verified(self):
        self.data['samples'][0]['color_profile_inspection'] = 'ICC_PRESENT'
        with self.assertRaises(ValueError): self.check()

    def test_android8_junit_runner_does_not_resolve_api28_decoder_signatures(self):
        android_test = Path(__file__).resolve().parents[1] / 'app/src/androidTest/kotlin/org/cajapersonal/fixmyshot'
        runner = (android_test / 'NativeGridDiagnosticTest.kt').read_text(encoding='utf-8')
        helper = (android_test / 'Api28GridDecoder.kt').read_text(encoding='utf-8')
        self.assertNotIn('android.graphics.ImageDecoder', runner)
        self.assertNotIn('ImageDecoder.decodeBitmap', runner)
        self.assertIn('Build.VERSION.SDK_INT >= 28', runner)
        self.assertIn('Api28GridDecoder.decode(file)', runner)
        self.assertIn('Geometry(96, 68, 65, 67, false)', runner)
        self.assertIn('Geometry(128, 96, 65, 67, false)', runner)
        self.assertIn('HeicCodecTrace.capture(writer)', runner)
        self.assertIn('HeicCodecTrace.pollBeforeStop(writer, trace)', runner)
        self.assertIn('@TargetApi(28)', helper)
        self.assertIn('ImageDecoder.decodeBitmap', helper)

    def test_instrumentation_failure(self):
        (self.root / 'grid-instrumentation.txt').write_text('OK (0 tests)\nINSTRUMENTATION_CODE: -1\n')
        with self.assertRaises(ValueError): self.check()


if __name__ == '__main__':
    unittest.main()
