#!/usr/bin/env python3
"""Validate both source-bound phases; synthetic URI data belongs to disposable emulators only."""
import argparse
import json
from pathlib import Path
import re
from urllib.parse import unquote, urlparse

PHASES = ('selectCancelAndPublishThroughActualFolderGrant',
          'afterForceStopPersistedGrantIsUsableAndRevocationDeniesReads')


def validate(directory, source, api, token):
    if not re.fullmatch(r'[0-9a-f]{40}', source) or api not in (26, 28) or not re.fullmatch(r'[0-9a-f]{32}', token):
        raise ValueError('Invalid expected fixture identity')
    for phase in PHASES:
        log = (directory / f'saf-{phase}.txt').read_text()
        if (not re.search(r'(?m)^OK \(1 test\)\s*$', log)
                or not re.search(r'(?m)^INSTRUMENTATION_CODE: -1\s*$', log)
                or any(x in log for x in ('FAILURES!!!', 'INSTRUMENTATION_FAILED', 'Process crashed', 'AssumptionViolated'))):
            raise ValueError('A real SAF phase did not complete')
    data = json.loads((directory / 'saf-provider-smoke.json').read_text())
    if (data.get('schema') != 1 or data.get('status') != 'PASS_REAL_SAF_EMULATOR'
            or data.get('api') != api or data.get('source_sha') != source or data.get('token') != token
            or data.get('provider') != 'com.android.externalstorage.documents'):
        raise ValueError('Incomplete or wrong-source SAF receipt')
    for flag in ('picker_cancelled', 'grant_read', 'grant_write', 'restart_readable',
                 'source_hashes_unchanged', 'duplicate_free', 'revoked_reads_denied'):
        if data.get(flag) is not True:
            raise ValueError('Missing executed SAF assertion: ' + flag)
    pids = [data.get('phase1_pid'), data.get('phase2_pid')]
    if any(type(x) is not int or x <= 0 for x in pids) or pids[0] == pids[1]:
        raise ValueError('Process restart not established')
    outputs = data.get('outputs')
    if not isinstance(outputs, list) or len(outputs) != 4:
        raise ValueError('Expected four real outputs')
    if (len({x.get('uri') for x in outputs}) != 4
            or sorted((x.get('mime'), x.get('side')) for x in outputs) !=
            [('image/png', 60), ('image/png', 64), ('image/webp', 60), ('image/webp', 64)]):
        raise ValueError('Duplicate or wrong output identities')
    for item in outputs:
        uri = urlparse(item.get('uri', ''))
        parts = [unquote(part) for part in uri.path.strip('/').split('/')]
        parent = 'primary:Download/FixMyShotSaf_' + token
        if (not re.fullmatch(r'[0-9a-f]{64}', item.get('sha256', ''))
                or uri.scheme != 'content' or uri.netloc != 'com.android.externalstorage.documents'
                or len(parts) != 4 or parts[0] != 'tree' or parts[1] != parent
                or parts[2] != 'document' or not parts[3].startswith(parent + '/')):
            raise ValueError('Invalid output receipt')
    return data


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('directory', type=Path)
    parser.add_argument('--source', required=True)
    parser.add_argument('--api', type=int, required=True)
    parser.add_argument('--token', required=True)
    args = parser.parse_args()
    validate(args.directory, args.source, args.api, args.token)
    print(f'PASS: actual SAF picker, 4 outputs, process restart and revoked reads; API {args.api}; emulator only')
