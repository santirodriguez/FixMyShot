#!/usr/bin/env python3
"""Check source-bound synthetic HEIC pixel-layout evidence; never certify format availability."""
import hashlib
import json
import math
from pathlib import Path
import re
import sys

NARROW = 'g1-grid-heic-66x68-off.heic'
WIDE = 'g1-grid-heic-96x68-off.heic'
HEX_SHA = re.compile(r'^[0-9a-f]{64}$')
SUCCESS = re.compile(r'(?m)^OK \(1 test\)\s*$')
DECODERS = ('BITMAP_FACTORY', 'IMAGE_DECODER')


def _object(value, label):
    if not isinstance(value, dict):
        raise ValueError(f'Invalid {label} object')
    return value


def _uint(value, lower, upper, label):
    if type(value) is not int or not lower <= value <= upper:
        raise ValueError(f'Invalid {label} integer')
    return value


def _score(value, label, upper=99.0):
    if type(value) not in (float, int) or not math.isfinite(value) or not 0 <= value <= upper:
        raise ValueError(f'Invalid {label} score')


def _hash(value, label):
    if not isinstance(value, str) or not HEX_SHA.fullmatch(value):
        raise ValueError(f'Invalid {label} digest')


def _ispe_width_offset(raw):
    i = raw.find(b'ispe')
    return i + 8 if i >= 0 and i + 12 <= len(raw) else None


def _width(raw, offset):
    return int.from_bytes(raw[offset:offset + 4], 'big')


def _positive(grid, width):
    matches = [s for s in grid['samples'] if s['format'] == 'HEIC' and
               s['width'] == width and s['height'] == 68 and s['grid'] is False]
    if len(matches) != 1:
        raise ValueError('Missing/duplicate source grid configuration')
    return matches[0]['status'] == 'ENCODED_AND_REOPENED'


def _image(info, width, label):
    info = _object(info, label)
    if info.get('width') != width or info.get('height') != 68:
        raise ValueError(f'Unexpected {label} image geometry')
    _uint(info.get('row_bytes'), width * 4, width * 32, f'{label} rowBytes')
    if info.get('config') not in ('ARGB_8888', 'RGB_565', 'RGBA_F16', 'UNKNOWN'):
        raise ValueError(f'Invalid {label} bitmap config')
    if type(info.get('decoded_srgb')) is not bool:
        raise ValueError(f'Invalid {label} decoded color descriptor')
    _hash(info.get('rgb_sha256'), f'{label} RGB')


def _comparison(comp):
    comp = _object(comp, 'same-decoder comparison')
    if comp.get('pixels') != 66 * 68:
        raise ValueError('Incorrect compared pixel count')
    for field in ('origin_psnr_db', 'best_crop_psnr_db', 'packed_row_psnr_db'):
        _score(comp.get(field), field)
    _uint(comp.get('best_crop_x'), 0, 30, 'best crop offset')
    _uint(comp.get('origin_exact_rgb_pixels'), 0, 66 * 68, 'exact RGB count')
    rows = comp.get('origin_row_mean_abs_rgb')
    if not isinstance(rows, list) or len(rows) != 68:
        raise ValueError('Missing row-wise pixel diagnostics')
    for i, value in enumerate(rows):
        _score(value, f'row {i} absolute RGB difference', 255.0)
    if comp['best_crop_psnr_db'] + 1e-7 < comp['origin_psnr_db']:
        raise ValueError('Best crop cannot score worse than origin')


def validate(directory: Path):
    log = (directory / 'heic-layout-instrumentation.txt').read_text(encoding='utf-8')
    if (not SUCCESS.search(log) or 'INSTRUMENTATION_CODE: -1' not in log or
            any(t in log for t in ('FAILURES!!!', 'Process crashed', 'INSTRUMENTATION_FAILED'))):
        raise ValueError('HEIC layout test did not complete')
    report = json.loads((directory / 'g1-heic-pixel-layout.json').read_text(encoding='utf-8'))
    if report.get('schema') != 1 or report.get('kind') != 'HEIC_PIXEL_LAYOUT_DIAGNOSTIC':
        raise ValueError('Wrong pixel-layout report schema/kind')
    api = _uint(report.get('api'), 26, 100, 'API')
    grid = json.loads((directory / 'g1-grid-diagnostic.json').read_text(encoding='utf-8'))
    if grid.get('api') != api or grid.get('status') != 'DIAGNOSTIC_COMPLETE':
        raise ValueError('Grid and pixel-layout API/source do not agree')
    narrow_positive, wide_positive = _positive(grid, 66), _positive(grid, 96)
    if api < 29:
        if report.get('status') != 'UNSUPPORTED_API' or narrow_positive or wide_positive:
            raise ValueError('Lower API claimed layout decoding or source encoder availability')
        if 'files' in report or 'decoders' in report or 'cross_decoder' in report:
            raise ValueError('Lower API fabricated layout evidence')
        return report
    if not (narrow_positive and wide_positive):
        if report.get('status') != 'UNAVAILABLE' or report.get('reason') != 'SOURCE_OUTPUT_UNAVAILABLE':
            raise ValueError('Unavailable grid pair masquerades as pixel layout measurement')
        if any(k in report for k in ('files', 'decoders', 'cross_decoder')):
            raise ValueError('Unavailable pair has fabricated output measurements')
        return report
    if report.get('status') != 'DIAGNOSTIC_COMPLETE':
        raise ValueError('Available grid pair lacks pixel-layout report')
    raw_n = (directory / NARROW).read_bytes()
    raw_w = (directory / WIDE).read_bytes()
    if not (16 <= len(raw_n) <= 2_097_152 and 16 <= len(raw_w) <= 2_097_152):
        raise ValueError('Invalid retained synthetic HEIC file size')
    files = _object(report.get('files'), 'source files')
    for label, raw in (('narrow', raw_n), ('wide', raw_w)):
        metadata = _object(files.get(label), label)
        if metadata.get('bytes') != len(raw) or metadata.get('sha256') != hashlib.sha256(raw).hexdigest():
            raise ValueError(f'{label} exact file hash/size differs from device report')
    difference = sum(a != b for a, b in zip(raw_n, raw_w)) + abs(len(raw_n) - len(raw_w))
    first = next((i for i, (a, b) in enumerate(zip(raw_n, raw_w)) if a != b), -1)
    if files.get('difference_count') != difference or files.get('first_difference_offset') != first:
        raise ValueError('Device-reported binary difference is incorrect')
    ni, wi = _ispe_width_offset(raw_n), _ispe_width_offset(raw_w)
    true_neighbor = (len(raw_n) == len(raw_w) and difference == 1 and ni is not None and
                     ni == wi and ni <= first < ni + 4 and
                     _width(raw_n, ni) == 66 and _width(raw_w, wi) == 96)
    if files.get('one_ispe_width_byte_difference') is not true_neighbor:
        raise ValueError('Fabricated one-byte container-width identity')
    decoders = report.get('decoders')
    if not isinstance(decoders, list) or len(decoders) != 2 or [d.get('decoder') for d in decoders] != list(DECODERS):
        raise ValueError('Missing/duplicate named decoding paths')
    measured = []
    for decoder in decoders:
        status = decoder.get('status')
        if status == 'MEASURED':
            _image(decoder.get('narrow'), 66, decoder['decoder'] + ' narrow')
            _image(decoder.get('wide'), 96, decoder['decoder'] + ' wide')
            _comparison(decoder.get('comparison'))
            measured.append(True)
        elif status == 'UNAVAILABLE':
            if decoder.get('reason') not in ('PIXEL_DECODE_FAILED', 'UNEXPECTED_VISIBLE_DIMENSIONS'):
                raise ValueError('Unexplained decoder-unavailable result')
            if decoder.get('reason') == 'UNEXPECTED_VISIBLE_DIMENSIONS':
                if not isinstance(decoder.get('narrow'), dict) or not isinstance(decoder.get('wide'), dict):
                    raise ValueError('Missing unexpected visible dimension observations')
            measured.append(False)
        else:
            raise ValueError('Invalid decoder status')
    cross = _object(report.get('cross_decoder'), 'cross-decoder comparison')
    if all(measured):
        if cross.get('status') != 'MEASURED':
            raise ValueError('Two decoded rasters not compared')
        for label, pixels in (('narrow', 66 * 68), ('wide', 96 * 68)):
            c = _object(cross.get(label), label)
            if c.get('pixels') != pixels:
                raise ValueError('Wrong cross-decoder pixel count')
            _uint(c.get('exact_rgb_pixels'), 0, pixels, label + ' exact count')
            _score(c.get('psnr_db'), label + ' PSNR')
            sha_equal = decoders[0][label]['rgb_sha256'] == decoders[1][label]['rgb_sha256']
            if sha_equal and c['exact_rgb_pixels'] != pixels:
                raise ValueError('Identical raster hashes but reported pixel mismatch')
    elif cross.get('status') != 'NOT_COMPARABLE':
        raise ValueError('Cross-decoder comparison claims missing rasters')
    return report


if __name__ == '__main__':
    try:
        value = validate(Path(sys.argv[1] if len(sys.argv) > 1 else 'evidence'))
    except (OSError, ValueError, KeyError, TypeError, UnicodeError) as error:
        sys.exit(f'FAIL: {error}')
    print(f"PASS: HEIC pixel-layout diagnostic API {value['api']}: {value['status']}; NOT production format acceptance")
