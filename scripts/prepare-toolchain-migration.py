#!/usr/bin/env python3
"""Prepare a source-pinned experimental checkout; never updates repository refs."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

DIST_SHA = '2ab2958f2a1e51120c326cad6f385153bb11ee93b3c216c5fccebfdfbb7ec6cb'
WRAPPER_SHA = '55243ef57851f12b070ad14f7f5bb8302daceeebc5bce5ece5fa6edb23e1145c'
SOURCE = 'ad075068564dc8a7d8d0eca35fc1f69ff3d16464'
TREE = 'b5ecadc6a756e0c07a67b7f0b38ae354744d3b8b'
EDITS = {
    'gradle/libs.versions.toml': [('agp = "8.13.2"', 'agp = "9.2.1"'),
        ('kotlin = "2.2.21"', 'kotlin = "2.4.21"'),
        ('kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }\n', '')],
    'build.gradle.kts': [('    alias(libs.plugins.kotlin.android) apply false\n', '')],
    'app/build.gradle.kts': [('    alias(libs.plugins.kotlin.android)\n', ''),
                           ('buildToolsVersion = "35.0.0"', 'buildToolsVersion = "36.0.0"')],
    'codec-native/build.gradle.kts': [('    alias(libs.plugins.kotlin.android)\n', ''),
                           ('buildToolsVersion = "35.0.0"', 'buildToolsVersion = "36.0.0"')],
    'scripts/setup-android.sh': [("'build-tools;35.0.0'", "'build-tools;36.0.0'")],
    'gradle/wrapper/gradle-wrapper.properties': [
        ('gradle-8.13-bin.zip', 'gradle-9.4.1-bin.zip'),
        ('20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78', DIST_SHA)],
}
LOCKS = ('app/gradle.lockfile', 'core/gradle.lockfile', 'codec-native/gradle.lockfile')


def git(root, *args):
    return subprocess.check_output(['git', '-C', str(root), *args], text=True).strip()


def prepare(root, request):
    if (request.get('schema') != 1 or request.get('source_commit') != SOURCE or
            request.get('source_tree') != TREE or request.get('scope') != 'EXPERIMENTAL_DERIVED_CHECKOUT' or
            set(request.get('inputs', {})) != set(EDITS) | set(LOCKS)):
        raise ValueError('Unapproved migration source or input set')
    if git(root, 'rev-parse', 'HEAD') != SOURCE or git(root, 'rev-parse', 'HEAD^{tree}') != TREE:
        raise ValueError('Checkout differs from the inspected source')
    if git(root, 'status', '--porcelain', '--untracked-files=all'):
        raise ValueError('Migration requires a pristine checkout')
    # Validate every input and replacement before any mutation.
    outputs = {}
    for name, expected in request['inputs'].items():
        path = root / name
        if path.is_symlink() or not path.is_file() or path.stat().st_size > 262144:
            raise ValueError('Missing, linked or oversized migration input')
        data = path.read_bytes()
        if hashlib.sha256(data).hexdigest() != expected:
            raise ValueError('Migration input differs: ' + name)
        if name in EDITS:
            text = data.decode('utf-8')
            for old, new in EDITS[name]:
                if text.count(old) != 1:
                    raise ValueError('Unexpected migration edit anchor: ' + name)
                text = text.replace(old, new)
            outputs[name] = text
    for name, text in outputs.items():
        (root / name).write_text(text)
    # Only the three version locks in this disposable, pinned checkout are reset.
    # Resolution regenerates them; they are not promoted to source automatically.
    for name in LOCKS:
        (root / name).unlink()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('request', type=Path)
    parser.add_argument('workspace', type=Path)
    parser.add_argument('--verify-generated-wrapper', action='store_true')
    args = parser.parse_args()
    if args.verify_generated_wrapper:
        if hashlib.sha256((args.workspace / 'gradle/wrapper/gradle-wrapper.jar').read_bytes()).hexdigest() != WRAPPER_SHA:
            raise SystemExit('Generated wrapper differs from the official Gradle9.4.1 anchor')
        print('PASS: generated wrapper JAR matches independent official checksum')
    else:
        if args.request.stat().st_size > 8192:
            raise SystemExit('Oversized migration request')
        prepare(args.workspace, json.loads(args.request.read_text()))
        print('Prepared EXPERIMENTAL_DERIVED_CHECKOUT; not a source-frozen build or security approval')
