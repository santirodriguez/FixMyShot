#!/usr/bin/env python3
"""Verify canonical legal bytes in an APK; never certifies license completeness."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = {
    'assets/legal/project/LICENSE': 'LICENSE',
    'assets/legal/THIRD_PARTY_NOTICES': 'THIRD_PARTY_NOTICES',
    'assets/legal/licenses/Apache-2.0.txt': 'LICENSES/Apache-2.0.txt',
    'assets/legal/licenses/Protobuf-BSD-3-Clause.txt': 'LICENSES/Protobuf-BSD-3-Clause.txt',
    'assets/legal/licenses/runtime-components.json': 'LICENSES/runtime-components.json',
    'assets/legal/licenses/Android-NDK-cpufeatures.txt': 'LICENSES/Android-NDK-cpufeatures.txt',
    'assets/licenses/libwebp/COPYING': 'codec-native/src/main/assets/licenses/libwebp/COPYING',
    'assets/licenses/libwebp/PATENTS': 'codec-native/src/main/assets/licenses/libwebp/PATENTS',
}


def inspect(apk_path, root=ROOT):
    receipts = []
    with zipfile.ZipFile(apk_path) as apk:
        for packaged, canonical in REQUIRED.items():
            matches = [info for info in apk.infolist() if info.filename == packaged]
            source = (root / canonical).read_bytes()
            if not source or len(source) > 262144:
                raise ValueError('Empty or oversized canonical legal text: ' + canonical)
            if len(matches) != 1 or matches[0].file_size != len(source):
                raise ValueError('Missing, duplicate or wrong-size legal asset: ' + packaged)
            data = apk.read(matches[0])
            if data != source:
                raise ValueError('Legal asset differs from canonical source: ' + packaged)
            receipts.append({'path': packaged, 'bytes': len(data),
                             'sha256': hashlib.sha256(data).hexdigest()})
    return {'scope': 'CANONICAL_LEGAL_BYTES_ONLY', 'status': 'PASS',
            'complete_third_party_license_review': False, 'files': receipts}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    args = parser.parse_args()
    print(json.dumps(inspect(args.apk), indent=2))
