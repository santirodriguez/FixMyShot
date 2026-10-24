#!/usr/bin/env python3
"""Validate an explicit unsigned rebuild request and preserve an exact byte comparison."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess

FIELDS = {'schema', 'source_commit', 'source_tree', 'baseline_run_id', 'baseline_artifact_id',
          'baseline_archive_sha256', 'expected_unsigned_apk_sha256', 'expected_unsigned_apk_bytes'}


def read_request(path):
    if path.stat().st_size > 8192:
        raise ValueError('Oversized rebuild request')
    def unique_fields(pairs):
        result = {}
        for name, item in pairs:
            if name in result:
                raise ValueError('Repeated rebuild request field')
            result[name] = item
        return result
    value = json.loads(path.read_text(), object_pairs_hook=unique_fields)
    if not isinstance(value, dict) or set(value) != FIELDS or type(value['schema']) is not int or value['schema'] != 1:
        raise ValueError('Unknown rebuild request schema or fields')
    for key, length in (('source_commit', 40), ('source_tree', 40), ('baseline_archive_sha256', 64),
                        ('expected_unsigned_apk_sha256', 64)):
        if not isinstance(value[key], str) or re.fullmatch('[0-9a-f]{' + str(length) + '}', value[key]) is None:
            raise ValueError('Invalid pinned identity or checksum')
    for key in ('baseline_run_id', 'baseline_artifact_id', 'expected_unsigned_apk_bytes'):
        if type(value[key]) is not int or value[key] <= 0:
            raise ValueError('Invalid baseline identity or size')
    if value['expected_unsigned_apk_bytes'] > 128 * 1024 * 1024:
        raise ValueError('Oversized baseline APK')
    return value


def git(root, *args):
    return subprocess.check_output(['git', '-C', str(root), *args], text=True).strip()


def verify_source(request, root, *, pristine):
    if Path(git(root, 'rev-parse', '--show-toplevel')).resolve() != root.resolve():
        raise ValueError('Rebuild directory is not its own repository root')
    if git(root, 'rev-parse', 'HEAD') != request['source_commit'] or git(root, 'rev-parse', 'HEAD^{tree}') != request['source_tree']:
        raise ValueError('Checked-out source differs from the pinned commit/tree')
    mode = '--untracked-files=all' if pristine else '--untracked-files=no'
    if git(root, 'status', '--porcelain', mode):
        raise ValueError('Rebuild source has unexpected changes')


def verify_fresh_start(root, cache):
    if cache.is_symlink() or (cache.exists() and (not cache.is_dir() or any(cache.iterdir()))):
        raise ValueError('Gradle user home is not empty')
    for name in ('.gradle', 'app/build', 'core/build', 'codec-native/build', 'codec-native/.cxx'):
        if (root / name).exists():
            raise ValueError('Rebuild starts with existing project outputs')


def compare(request, root, apk, control_commit):
    if re.fullmatch('[0-9a-f]{40}', control_commit or '') is None:
        raise ValueError('Invalid workflow control commit')
    verify_source(request, root, pristine=False)
    if not apk.is_file() or apk.is_symlink() or apk.stat().st_size > 128 * 1024 * 1024:
        raise ValueError('Invalid rebuilt APK')
    digest = hashlib.sha256()
    with apk.open('rb') as stream:
        for block in iter(lambda: stream.read(64 * 1024), b''):
            digest.update(block)
    actual = digest.hexdigest()
    size = apk.stat().st_size
    return {'schema': 1, 'scope': 'UNSIGNED_APK_BYTE_COMPARISON',
            'workflow_control_commit': control_commit, 'request': request,
            'source_identity_checked': True, 'actual_unsigned_apk_sha256': actual,
            'actual_unsigned_apk_bytes': size,
            'byte_identical': actual == request['expected_unsigned_apk_sha256'] and size == request['expected_unsigned_apk_bytes'],
            'cache_policy': 'REQUIRES_VERIFY_STEP_AND_WORKFLOW_REVIEW',
            'dependency_authentication': 'OPEN', 'production_signature': 'NOT_RUN',
            'physical_acceptance': 'NOT_RUN'}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=('request', 'verify', 'compare'))
    parser.add_argument('request', type=Path)
    parser.add_argument('--github-output', type=Path)
    parser.add_argument('--workspace', type=Path)
    parser.add_argument('--cache', type=Path)
    parser.add_argument('--apk', type=Path)
    parser.add_argument('--report', type=Path)
    parser.add_argument('--control-commit')
    args = parser.parse_args()
    request = read_request(args.request)
    if args.action == 'request':
        if args.github_output:
            with args.github_output.open('a') as output:
                output.write('source_commit=' + request['source_commit'] + '\n')
        print(json.dumps(request, indent=2))
    elif args.action == 'verify':
        if args.workspace is None or args.cache is None:
            parser.error('verify requires workspace and cache')
        verify_source(request, args.workspace, pristine=True)
        verify_fresh_start(args.workspace, args.cache)
        print('PASS: exact pinned source, pristine worktree and empty Gradle/project caches')
    else:
        if args.workspace is None or args.apk is None or args.report is None:
            parser.error('compare requires workspace, apk and report')
        result = compare(request, args.workspace, args.apk, args.control_commit)
        args.report.write_text(json.dumps(result, indent=2) + '\n')
        print(json.dumps(result, indent=2))
        if not result['byte_identical']:
            raise SystemExit('FAIL: unsigned APK bytes differ; report preserved for diagnosis')


if __name__ == '__main__':
    main()
