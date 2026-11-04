#!/usr/bin/env python3
"""Check actual pinned NDK source bytes and their packaged notices without executing them."""
import argparse
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REVISION = '28.2.13676358'
UPSTREAM = 'https://android.googlesource.com/platform/ndk/+/805836ef53231ef86dd5fcc5652a1c28d74f4bc4/sources/android/cpufeatures/'
EXPECTED = {'cpu-features.c': '1b8a88eb7d3a5437d17e12475539398d3e1c5eef13d85941679841254af0a8f5', 'cpu-features.h': 'f4989fabece7ea538e3c1272dca47f8a9a4423a87c79a1344a2611e884cb7319', 'NOTICE': 'c870d52f42a8f93023b99a85fe68d02637d6b04e5f35bbcd7f98759adb4e4d49'}


def inspect(ndk, canonical):
    properties = (ndk / 'source.properties').read_bytes()
    if len(properties) > 8192 or 'Pkg.Revision = '+REVISION not in properties.decode().splitlines():
        raise ValueError('Unexpected NDK package revision')
    files = {}
    for name, expected in EXPECTED.items():
        data = (ndk / 'sources/android/cpufeatures' / name).read_bytes()
        if len(data) > 131072 or hashlib.sha256(data).hexdigest() != expected:
            raise ValueError('Actual NDK source differs from pinned release: '+name)
        files[name] = data
    def header(data):
        if not data.startswith(b'/*\n'):
            raise ValueError('Unexpected upstream copyright framing')
        return data[:data.index(b' */')+4]
    c, h = header(files['cpu-features.c']), header(files['cpu-features.h'])
    if c != h or canonical != c+b'\n\n'+files['NOTICE']:
        raise ValueError('Packaged native attribution differs from actual NDK source notices')
    report = {'schema':1, 'scope':'PINNED_NDK_CPUFEATURES_SOURCE_AND_CANONICAL_NOTICES',
              'status':'PASS', 'ndk_revision':REVISION, 'upstream':UPSTREAM,
              'files':[{'path':n,'bytes':len(b),'sha256':hashlib.sha256(b).hexdigest()} for n,b in files.items()],
              'canonical_sha256':hashlib.sha256(canonical).hexdigest(),
              'complete_native_or_license_review':False, 'pgp_signature_claim':False}
    return report, files, properties


if __name__ == '__main__':
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('ndk',type=Path);p.add_argument('output',type=Path);a=p.parse_args()
    report, files, properties=inspect(a.ndk,(ROOT/'LICENSES/Android-NDK-cpufeatures.txt').read_bytes())
    a.output.mkdir(parents=True,exist_ok=False)
    for name,data in files.items():(a.output/name).write_bytes(data)
    (a.output/'source.properties').write_bytes(properties)
    (a.output/'report.json').write_text(json.dumps(report,indent=2)+'\n')
    print(json.dumps(report))
