#!/usr/bin/env python3
"""Identify repackaged library symbols; do not claim method-body or source equivalence."""
import argparse
import hashlib
import io
import json
from pathlib import Path
import struct
import zipfile


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def symbols(raw):
    if len(raw) < 10 or len(raw) > 16*1024*1024 or raw[:4] != b'\xca\xfe\xba\xbe':
        raise ValueError('Invalid class input')
    pos = 8

    def take(n):
        nonlocal pos
        if n < 0 or pos+n > len(raw):
            raise ValueError('Truncated class input')
        data = raw[pos:pos+n]
        pos += n
        return data

    def u2():
        return struct.unpack('>H', take(2))[0]

    def u4():
        return struct.unpack('>I', take(4))[0]

    cp = [None]*u2()
    i = 1
    while i < len(cp):
        tag = take(1)[0]
        if tag == 1:
            value = take(u2()).replace(b'com/google/flatbuffers', b'androidx/emoji2/text/flatbuffer').replace(b'com.google.flatbuffers', b'androidx.emoji2.text.flatbuffer')
            cp[i] = (tag, value.hex())
        elif tag in (3, 4, 5, 6):
            cp[i] = (tag, take(8 if tag in (5, 6) else 4).hex())
        elif tag in (7, 8, 16, 19, 20):
            cp[i] = (tag, u2())
        elif tag in (9, 10, 11, 12, 17, 18):
            cp[i] = (tag, u2(), u2())
        elif tag == 15:
            cp[i] = (tag, take(1)[0], u2())
        else:
            raise ValueError('Unknown constant tag')
        if tag in (5, 6):
            i += 1
        i += 1

    cache = {}

    def resolve(index, stack=()):
        if not 0 < index < len(cp) or cp[index] is None or index in stack or len(stack) > 16:
            raise ValueError('Invalid or cyclic constant reference')
        if index in cache:
            return cache[index]
        tag, *items = cp[index]
        if tag in (1, 3, 4, 5, 6):
            result = (tag, *items)
        elif tag in (7, 8, 9, 10, 11, 12, 16, 19, 20):
            result = (tag, *(resolve(x, stack+(index,)) for x in items))
        elif tag in (15, 17, 18):
            result = (tag, items[0], resolve(items[1], stack+(index,)))
        cache[index] = result
        return result

    def utf8(index):
        value = resolve(index)
        if value[0] != 1:
            raise ValueError('Expected UTF8 constant')
        return bytes.fromhex(value[1]).decode('utf-8')

    def attributes():
        found = []
        for _ in range(u2()):
            name = utf8(u2())
            data = take(u4())
            if name == 'ConstantValue':
                if len(data) != 2:
                    raise ValueError('Invalid ConstantValue attribute')
                found.append((name, resolve(struct.unpack('>H', data)[0])))
            # Code and every other attribute are deliberately not compared.
        return found

    access, this, parent = u2(), u2(), u2()
    if resolve(this)[0] != 7 or parent and resolve(parent)[0] != 7:
        raise ValueError('Invalid class/super reference')
    interfaces = [resolve(u2()) for _ in range(u2())]
    members = []
    for kind in ('fields', 'methods'):
        rows = []
        for _ in range(u2()):
            flags, name, descriptor = u2(), utf8(u2()), utf8(u2())
            rows.append((flags, name, descriptor, attributes()))
        members.append((kind, sorted(rows)))
    attributes()
    if pos != len(raw):
        raise ValueError('Trailing class input')
    constants = sorted({json.dumps(resolve(i), separators=(',', ':')) for i in range(1, len(cp)) if cp[i] is not None})
    layout = {'version': list(struct.unpack_from('>2H', raw, 4)), 'access': access,
              'class': resolve(this), 'super': resolve(parent) if parent else None,
              'interfaces': interfaces, 'members': members}
    return {'layout': layout, 'constants': constants, 'constant_slots': len(cp)-1}


def compare(left, right):
    a, b = symbols(left), symbols(right)
    def digest(value):
        return sha(json.dumps(value, sort_keys=True, separators=(',', ':')).encode())
    return {'class_bytes_identical': left == right,
            'declaration_layout_identical': a['layout'] == b['layout'],
            'unique_resolved_constant_set_identical': a['constants'] == b['constants'],
            'original_layout_sha256': digest(a['layout']), 'frozen_layout_sha256': digest(b['layout']),
            'original_constant_set_sha256': digest(a['constants']), 'frozen_constant_set_sha256': digest(b['constants']),
            'original_constant_slots': a['constant_slots'], 'frozen_constant_slots': b['constant_slots'],
            'method_bodies_compared': False, 'other_attributes_compared': False,
            'source_binary_equivalence': False}


def read_jar(raw):
    with zipfile.ZipFile(io.BytesIO(raw)) as z:
        items = z.infolist()
        if len(items) > 10000 or len({x.filename for x in items}) != len(items) or sum(x.file_size for x in items) > 32*1024*1024:
            raise ValueError('Jar bounds or duplicates')
        return {x.filename: z.read(x) for x in items if x.filename.endswith('.class')}


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    for name in ('original_jar', 'frozen_aar', 'output'):
        p.add_argument(name, type=Path)
    p.add_argument('--expected-original', required=True)
    p.add_argument('--expected-frozen', required=True)
    a = p.parse_args()
    if a.output.exists():
        raise ValueError('Output must be fresh')
    original, frozen = a.original_jar.read_bytes(), a.frozen_aar.read_bytes()
    if len(original) > 32*1024*1024 or len(frozen) > 32*1024*1024 or sha(original) != a.expected_original or sha(frozen) != a.expected_frozen:
        raise ValueError('Archive differs from explicit pin')
    source = read_jar(original)
    with zipfile.ZipFile(io.BytesIO(frozen)) as z:
        if z.namelist().count('libs/repackaged.jar') != 1:
            raise ValueError('Ambiguous repackaged Jar')
        target = read_jar(z.read('libs/repackaged.jar'))
    rows = []
    used = set()
    for name, raw in sorted(source.items()):
        mapped = name.replace('com/google/flatbuffers', 'androidx/emoji2/text/flatbuffer')
        if mapped not in target:
            raise ValueError('Published class absent from repackaged Jar')
        used.add(mapped)
        rows.append({'original': name, 'frozen': mapped, **compare(raw, target[mapped])})
    report = {'schema': 1, 'scope': 'DECLARATIONS_AND_RESOLVED_CONSTANT_SETS_AFTER_DECLARED_NAMESPACE_MAPPING_ONLY',
              'original_sha256': sha(original), 'frozen_aar_sha256': sha(frozen),
              'producer_sha256': sha(Path(__file__).read_bytes()), 'rows': rows,
              'additional_repackaged_classes': sorted(set(target)-used),
              'method_body_other_attributes_and_source_equivalence': False,
              'publisher_authentication_changed': False, 'whole_distribution_approved': False}
    with a.output.open('x') as f:
        json.dump(report, f, indent=2)
        f.write('\n')
    print(json.dumps({'classes': len(rows), 'matching_layouts': sum(r['declaration_layout_identical'] for r in rows),
                      'matching_constant_sets': sum(r['unique_resolved_constant_set_identical'] for r in rows),
                      'extra_classes': report['additional_repackaged_classes']}))
