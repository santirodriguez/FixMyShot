#!/usr/bin/env python3
"""Check a bounded encoded 65x67 synthetic AVIF against the product fixture.

Requires ffprobe and ffmpeg, not a platform codec. This is a diagnostic gate,
not proof of Android quality or product output approval.
"""
import argparse
import json
import math
from pathlib import Path
import statistics
import subprocess
import sys

W, H = 65, 67
COLORS = ((64, 64, 64), (128, 128, 128), (192, 192, 192),
          (192, 64, 64), (64, 192, 64), (64, 64, 192))


def fixture_pixel(x, y):
    if y < H // 2:
        return COLORS[min(5, x * len(COLORS) // W)]
    if y < H * 3 // 4:
        return (255, 255, 255) if y % 16 == 0 or x % 32 == 0 else (18, 18, 18)
    if x % 32 in (0, 1) and y % 16 <= 10:
        return (20, 100, 220)
    return (255, 255, 255)


def read_sample(path):
    if path.stat().st_size not in range(20, 16385):
        raise ValueError('Unbounded or invalid candidate size')
    raw = path.read_bytes()
    if raw[4:8] != b'ftyp' or b'avif' not in raw[:128]:
        raise ValueError('Missing AVIF container brand')
    meta = subprocess.run([
        'ffprobe', '-v', 'error', '-select_streams', 'v:0',
        '-show_entries', 'stream=width,height,pix_fmt,color_range',
        '-of', 'json', str(path)
    ], check=True, capture_output=True)
    stream = json.loads(meta.stdout)['streams'][0]
    if (stream['width'], stream['height']) != (W, H):
        raise ValueError(f'Dimensions not preserved: {stream}')
    if stream['pix_fmt'] != 'yuv444p':
        raise ValueError(f'Not a 4:4:4 sample: {stream}')
    decoded = subprocess.run(['ffmpeg', '-v', 'error', '-i', str(path),
        '-frames:v', '1', '-f', 'rawvideo', '-pix_fmt', 'rgb24', '-'],
        check=True, capture_output=True).stdout
    if len(decoded) != W * H * 3:
        raise ValueError('Unexpected decoder output bytes')
    return stream, decoded


def metrics(decoded):
    squares = 0
    patch = 0
    for y in range(H):
        for x in range(W):
            expected = fixture_pixel(x, y)
            start = (y * W + x) * 3
            squares += sum((expected[c] - decoded[start + c])**2 for c in range(3))
    for idx, color in enumerate(COLORS):
        cx = max(1, min(W-2, W * (2 * idx + 1) // (2 * len(COLORS))))
        cy = H // 4
        for c in range(3):
            samples = [decoded[(y*W + x)*3+c] for y in range(cy-1,cy+2)
                       for x in range(cx-1,cx+2)]
            patch = max(patch, abs(statistics.median(samples) - color[c]))
    mse = squares / (W*H*3)
    return {'psnr_db': round(10*math.log10(255**2/mse), 4) if mse else None,
            'max_patch_error': patch, 'passes_numeric_target': mse == 0 or
            (10*math.log10(255**2/mse)>=38 and patch<=2)}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('file', type=Path)
    a = p.parse_args()
    stream, pixels = read_sample(a.file)
    outcome = {'geometry': [W,H], 'stream': stream, 'metrics': metrics(pixels),
               'scope': 'HOST_ONLY_REFERENCE__NOT_PIXEL_OR_PRODUCTION'}
    print(json.dumps(outcome, indent=2))
    return 0 if outcome['metrics']['passes_numeric_target'] else 1


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (ValueError, KeyError, subprocess.CalledProcessError) as error:
        print(f'INVALID_OR_UNQUALIFIED_AVIF: {error}', file=sys.stderr)
        sys.exit(2)
