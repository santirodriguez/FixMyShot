#!/usr/bin/env python3
"""Host-only regression tests for synthetic HEIC pixel-layout evidence validation."""
import hashlib
import importlib.util
import json
from pathlib import Path
import struct
import tempfile
import unittest

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('heic_layout_checker', HERE / 'check-heic-layout.py')
checker = importlib.util.module_from_spec(spec)
spec.loader.exec_module(checker)
SUCCESS = 'INSTRUMENTATION_RESULT: stream=\n\nOK (1 test)\n\nINSTRUMENTATION_CODE: -1\n'


def mock_heic(width):
    # Synthetic validation bytes, NOT a codec-produced HEIC or fidelity fixture.
    return (struct.pack('>I', 20) + b'ftypheic' + struct.pack('>I', 0) + b'mif1'
            + struct.pack('>I', 20) + b'ispe' + bytes(4)
            + struct.pack('>I', width) + struct.pack('>I', 68)
            + struct.pack('>I', 12) + b'mdat' + b'data')


def image(width, digest):
    return dict(width=width, height=68, row_bytes=width * 4, config='ARGB_8888',
                decoded_srgb=True, rgb_sha256=digest)


def metrics():
    return dict(pixels=66 * 68, origin_psnr_db=18.0, best_crop_x=3,
                best_crop_psnr_db=21.0, packed_row_psnr_db=25.0,
                origin_exact_rgb_pixels=8, origin_row_mean_abs_rgb=[13.5] * 68)


def decoder(name, a='0', b='1'):
    return dict(decoder=name, status='MEASURED', narrow=image(66, a * 64),
                wide=image(96, b * 64), comparison=metrics())


def grid(api=36, positive=True):
    return dict(schema=2, api=api, status='DIAGNOSTIC_COMPLETE',
                samples=[dict(format='HEIC', width=w, height=68, grid=False,
                              status='ENCODED_AND_REOPENED' if positive else 'UNAVAILABLE')
                         for w in (66, 96)])


class PixelLayoutValidationTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        (self.root / 'heic-layout-instrumentation.txt').write_text(SUCCESS)
        self.grid = grid()
        self.n = mock_heic(66)
        self.w = mock_heic(96)
        (self.root / checker.NARROW).write_bytes(self.n)
        (self.root / checker.WIDE).write_bytes(self.w)
        d = next(i for i, (a, b) in enumerate(zip(self.n, self.w)) if a != b)
        self.assertEqual(1, sum(a != b for a, b in zip(self.n, self.w)))
        self.report = dict(schema=1, kind='HEIC_PIXEL_LAYOUT_DIAGNOSTIC', api=36,
                           status='DIAGNOSTIC_COMPLETE',
                           files=dict(narrow=dict(bytes=len(self.n), sha256=hashlib.sha256(self.n).hexdigest()),
                                      wide=dict(bytes=len(self.w), sha256=hashlib.sha256(self.w).hexdigest()),
                                      difference_count=1, first_difference_offset=d,
                                      one_ispe_width_byte_difference=True),
                           decoders=[decoder('BITMAP_FACTORY'), decoder('IMAGE_DECODER', '2', '3')],
                           cross_decoder=dict(status='MEASURED',
                                              narrow=dict(exact_rgb_pixels=12, pixels=66 * 68, psnr_db=17.0),
                                              wide=dict(exact_rgb_pixels=18, pixels=96 * 68, psnr_db=16.0)))

    def check(self):
        (self.root / 'g1-grid-diagnostic.json').write_text(json.dumps(self.grid))
        (self.root / 'g1-heic-pixel-layout.json').write_text(json.dumps(self.report))
        return checker.validate(self.root)

    def test_two_decoders_with_real_file_hash_and_one_ispe_byte_change(self):
        self.assertEqual(self.check()['status'], 'DIAGNOSTIC_COMPLETE')

    def test_api26_has_no_native_decoder_claim(self):
        self.grid = grid(26, False)
        self.report = dict(schema=1, kind='HEIC_PIXEL_LAYOUT_DIAGNOSTIC', api=26,
                           status='UNSUPPORTED_API')
        self.check()
        self.report['decoders'] = []
        with self.assertRaises(ValueError): self.check()

    def test_grid_output_unavailable_is_not_faked(self):
        self.grid = grid(36, False)
        self.report = dict(schema=1, kind='HEIC_PIXEL_LAYOUT_DIAGNOSTIC', api=36,
                           status='UNAVAILABLE', reason='SOURCE_OUTPUT_UNAVAILABLE')
        self.check()
        self.report['status'] = 'DIAGNOSTIC_COMPLETE'
        with self.assertRaises(ValueError): self.check()

    def test_file_mutation_and_false_ispe_claim_rejected(self):
        self.report['files']['one_ispe_width_byte_difference'] = False
        with self.assertRaises(ValueError): self.check()
        self.report['files']['one_ispe_width_byte_difference'] = True
        (self.root / checker.NARROW).write_bytes(self.n + b'tampered')
        with self.assertRaises(ValueError): self.check()

    def test_bad_geometry_and_missing_route_rejected(self):
        self.report['decoders'][0]['narrow']['width'] = 65
        with self.assertRaises(ValueError): self.check()
        self.report['decoders'][0]['narrow']['width'] = 66
        self.report['decoders'].pop()
        with self.assertRaises(ValueError): self.check()

    def test_fabricated_crop_and_row_metrics_rejected(self):
        comp = self.report['decoders'][0]['comparison']
        comp['best_crop_psnr_db'] = comp['origin_psnr_db'] - 1.0
        with self.assertRaises(ValueError): self.check()
        comp['best_crop_psnr_db'] = 21.0
        comp['origin_row_mean_abs_rgb'].pop()
        with self.assertRaises(ValueError): self.check()

    def test_missing_pair_or_cross_decoder_result_rejected(self):
        self.report['cross_decoder'] = {'status': 'NOT_COMPARABLE'}
        with self.assertRaises(ValueError): self.check()
        self.report['decoders'][1] = dict(decoder='IMAGE_DECODER',
                                          status='UNAVAILABLE', reason='PIXEL_DECODE_FAILED')
        self.check()

    def test_instrumentation_must_actually_finish(self):
        (self.root / 'heic-layout-instrumentation.txt').write_text('OK (0 tests)\nINSTRUMENTATION_CODE: -1\n')
        with self.assertRaises(ValueError): self.check()

    def test_api26_junit_class_does_not_load_api28_decoder_signatures(self):
        source = (HERE.parent / 'app/src/androidTest/kotlin/org/cajapersonal/fixmyshot'
                  / 'NativeHeicPixelLayoutTest.kt').read_text()
        helper = (HERE.parent / 'app/src/androidTest/kotlin/org/cajapersonal/fixmyshot'
                  / 'Api28GridDecoder.kt').read_text()
        self.assertNotIn('android.graphics.ImageDecoder', source)
        self.assertIn('Api28GridDecoder.decode(file)', source)
        self.assertIn('Build.VERSION.SDK_INT < 29', source)
        self.assertIn('ImageDecoder.decodeBitmap', helper)
        self.assertIn('wide.pixels[y * w + x]', source)  # row-major interpretation


if __name__ == '__main__':
    unittest.main()
