#!/usr/bin/env python3
"""Read actual APK DEX definitions/references for the frozen scan's tooling package families."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import struct
import zipfile
import zlib

PREFIXES = {
    'netty': b'Lio/netty/',
    'commons_lang3': b'Lorg/apache/commons/lang3/',
    'apache_httpclient': b'Lorg/apache/http/',
    'jose4j': b'Lorg/jose4j/',
    'bouncycastle': b'Lorg/bouncycastle/',
    'jdom2': b'Lorg/jdom2/',
}
MAX_DEX = 64 * 1024 * 1024


def dex_types(data):
    if (len(data) < 112 or len(data) > MAX_DEX or data[:8] not in
            [b'dex\n'+v+b'\0' for v in (b'035', b'037', b'038', b'039', b'040')]):
        raise ValueError('Unsupported or oversized DEX')
    def u32(offset):
        if offset < 0 or offset+4 > len(data):
            raise ValueError('DEX integer out of bounds')
        return struct.unpack_from('<I', data, offset)[0]
    if (u32(32) != len(data) or u32(36) != 112 or u32(40) != 0x12345678 or
            data[12:32] != hashlib.sha1(data[32:]).digest() or u32(8) != zlib.adler32(data[12:])):
        raise ValueError('DEX header, size or integrity differs')
    def table(offset, width):
        count, start = u32(offset), u32(offset+4)
        if count > 1_000_000 or start > len(data) or (count and start < 112) or count*width > len(data)-start:
            raise ValueError('DEX table out of bounds')
        return count, start
    strings, string_start = table(56, 4)
    types, type_start = table(64, 4)
    classes, class_start = table(96, 32)
    descriptors = []
    for index in range(types):
        string_index = u32(type_start+index*4)
        if string_index >= strings:
            raise ValueError('DEX type string index out of bounds')
        start = u32(string_start+string_index*4)
        if start < 112:
            raise ValueError('DEX string data out of bounds')
        for _ in range(5):
            if start >= len(data):
                raise ValueError('Truncated DEX string length')
            byte = data[start]
            start += 1
            if not byte & 128:
                break
        else:
            raise ValueError('Oversized DEX string length')
        end = data.find(b'\0', start, min(start+4097, len(data)))
        if end < 0:
            raise ValueError('Unterminated or excessive DEX type descriptor')
        descriptors.append(data[start:end])
    definitions = []
    for index in range(classes):
        type_index = u32(class_start+index*32)
        if type_index >= types:
            raise ValueError('DEX class index out of bounds')
        definitions.append(descriptors[type_index])
    return definitions, descriptors


def inspect(apk, source_commit):
    if re.fullmatch('[0-9a-f]{40}', source_commit) is None or apk.stat().st_size > 128*1024*1024:
        raise ValueError('Invalid source identity or oversized APK')
    definitions, references, dex_files = [], [], []
    with zipfile.ZipFile(apk) as archive:
        entries = archive.infolist()
        if len(entries) > 16384 or len(entries) != len({x.filename for x in entries}):
            raise ValueError('Repeated or excessive APK entries')
        dex_entries = [x for x in entries if re.fullmatch(r'classes(?:[2-9]|[1-9][0-9]+)?\.dex', x.filename)]
        if not dex_entries or sum(x.file_size for x in dex_entries) > 128*1024*1024:
            raise ValueError('Missing or excessive DEX payload')
        for entry in dex_entries:
            if entry.file_size > MAX_DEX:
                raise ValueError('Oversized DEX member')
            data = archive.read(entry)
            defined, referenced = dex_types(data)
            definitions.extend(defined)
            references.extend(referenced)
            dex_files.append({'path':entry.filename, 'bytes':len(data), 'sha256':hashlib.sha256(data).hexdigest(),
                              'class_definitions':len(defined), 'type_references':len(referenced)})
    families = {name:{'class_definitions':sum(x.startswith(prefix) for x in definitions),
                      'type_references':sum(x.lstrip(b'[').startswith(prefix) for x in references)}
                for name,prefix in PREFIXES.items()}
    matches = any(x['class_definitions'] or x['type_references'] for x in families.values())
    return {'schema':1, 'scope':'EXACT_APK_DEX_DEFINITIONS_AND_TYPE_REFERENCES_ONLY',
            'source_commit':source_commit, 'apk_bytes':apk.stat().st_size,
            'apk_sha256':hashlib.sha256(apk.read_bytes()).hexdigest(),
            'status':'MATCHES_REQUIRE_REVIEW' if matches else 'NO_MATCHING_PACKAGED_TYPES',
            'dex_files':dex_files, 'families':families, 'reflection_or_dynamic_loading_coverage':False,
            'build_tool_risk_closed':False, 'whole_security_or_exploitability_approval':False}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--source-commit', required=True)
    args = parser.parse_args()
    print(json.dumps(inspect(args.apk, args.source_commit), indent=2))
