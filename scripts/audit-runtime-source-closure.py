#!/usr/bin/env python3
"""Attribute APK class names to exact frozen archives and source companions, offline.

Names and SourceFile attributes establish source-material coverage, not semantic
or binary/source equivalence, publisher trust, or final license clearance.
"""
import argparse
import collections
import hashlib
import importlib.util
import io
import json
from pathlib import Path, PurePosixPath
import re
import struct
import zipfile
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location('apk_types', Path(__file__).with_name('audit-apk-tooling-exposure.py'))
dex = importlib.util.module_from_spec(spec)
spec.loader.exec_module(dex)


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def class_source(data):
    if len(data) > 16*1024*1024 or data[:4] != b'\xca\xfe\xba\xbe':
        raise ValueError('Invalid class file')
    pos = 8

    def take(n):
        nonlocal pos
        if n < 0 or pos+n > len(data):
            raise ValueError('Truncated class file')
        b = data[pos:pos+n]
        pos += n
        return b

    def u2():
        return struct.unpack('>H', take(2))[0]

    def u4():
        return struct.unpack('>I', take(4))[0]

    cp = [None]*u2()
    i = 1
    while i < len(cp):
        tag = take(1)[0]
        if tag == 1:
            cp[i] = take(u2())
        elif tag == 7:
            cp[i] = u2()
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            take(4)
        elif tag in (5, 6):
            take(8)
            i += 1
        elif tag in (8, 16, 19, 20):
            take(2)
        elif tag == 15:
            take(3)
        else:
            raise ValueError('Unknown class constant tag')
        i += 1

    def text(index):
        if not 0 < index < len(cp) or not isinstance(cp[index], bytes):
            raise ValueError('Invalid class UTF8 constant')
        return cp[index]

    take(2)
    this = u2()
    if not 0 < this < len(cp) or not isinstance(cp[this], int):
        raise ValueError('Invalid class name reference')
    name = text(cp[this]).decode('utf-8')
    take(2)
    take(u2()*2)

    def attributes(source=False):
        found = []
        for _ in range(u2()):
            key, size = text(u2()), u4()
            raw = take(size)
            if source and key == b'SourceFile':
                if size != 2:
                    raise ValueError('Invalid SourceFile size')
                found.append(text(struct.unpack('>H', raw)[0]).decode('utf-8'))
        if len(found) > 1:
            raise ValueError('Repeated SourceFile attribute')
        return found[0] if found else None

    for _ in range(2):
        for _ in range(u2()):
            take(6)
            attributes()
    source = attributes(True)
    if pos != len(data):
        raise ValueError('Trailing class bytes')
    return name, source


def safe_zip(raw):
    z = zipfile.ZipFile(io.BytesIO(raw))
    items = z.infolist()
    if len(items) > 30000 or len({x.filename for x in items}) != len(items) or sum(x.file_size for x in items) > 256*1024*1024:
        raise ValueError('Archive bounds or duplicates')
    for x in items:
        p = PurePosixPath(x.filename)
        if p.is_absolute() or '..' in p.parts or '\\' in x.filename or (x.external_attr >> 16) & 0o170000 == 0o120000:
            raise ValueError('Unsafe archive path')
    return z


def source_keys(raw):
    keys = set()
    with safe_zip(raw) as z:
        for x in z.infolist():
            if x.filename.endswith(('.java', '.kt')):
                body = z.read(x)
                match = re.search(rb'^\s*package\s+([A-Za-z0-9_.]+)', body, re.M)
                if match:
                    keys.add((match[1].decode().replace('.', '/'), PurePosixPath(x.filename).name))
                    jvm = re.search(rb'@file:(?:kotlin\.jvm\.)?JvmPackageName\("([A-Za-z0-9_.]+)"\)', body)
                    if jvm:
                        keys.add((jvm[1].decode().replace('.', '/'), PurePosixPath(x.filename).name))
    return keys


def archive_classes(raw, nested=True):
    result = {}
    with safe_zip(raw) as z:
        for x in z.infolist():
            if x.filename.endswith('.class'):
                name, source = class_source(z.read(x))
                if x.filename != name+'.class' and not x.filename.startswith('META-INF/versions/'):
                    raise ValueError('Class internal name differs from archive path')
                if name != 'module-info':
                    if name in result:
                        raise ValueError('Ambiguous class in archive')
                    result[name] = source
            elif nested and (x.filename == 'classes.jar' or x.filename.startswith('libs/') and x.filename.endswith('.jar')):
                more = archive_classes(z.read(x), False)
                if set(more) & set(result):
                    raise ValueError('Repeated nested class')
                result.update(more)
    return result


def attribution(name, owners, resource_namespaces=None):
    if name in owners:
        return 'FROZEN_CLASS_NAME', name
    # D8 appends these suffixes to the originating class name. This is a name
    # relation only; it does not prove generated bytecode equals original code.
    for delimiter in ('$$ExternalSynthetic', '$-CC', '$-EL'):
        if delimiter in name and name.split(delimiter, 1)[0] in owners:
            return 'D8_GENERATED_NAME_RELATION', name.split(delimiter, 1)[0]
    if name.endswith('-IA') and name[:-3] in owners:
        return 'D8_GENERATED_NAME_RELATION', name[:-3]
    namespace, _, leaf = name.rpartition('/')
    resource = (resource_namespaces or {}).get(namespace)
    if resource and (leaf == 'R' or leaf.startswith('R$') and resource['r_symbols_present']):
        return 'AAR_NAMESPACE_RESOURCE_CLASS_NAME', None
    if name.startswith('org/cajapersonal/fixmyshot/'):
        return 'ACCEPTED_PROJECT_NAMESPACE', None
    return 'UNATTRIBUTED_REQUIRES_REVIEW', None


def audit(apk, binary_plan, binaries, capture, source_root, protobuf, expected_apk):
    raw_apk = apk.read_bytes()
    if len(raw_apk) > 128*1024*1024 or sha(raw_apk) != expected_apk:
        raise ValueError('APK differs from explicit accepted hash')
    sources = {r['coordinate']: r for r in capture['components']}
    owners = collections.defaultdict(list)
    resource_namespaces = {}
    source_material = {}
    for row in binary_plan:
        coordinate = row['coordinate']
        raw = (binaries/row['sha256']).read_bytes()
        if len(raw) != row['bytes'] or sha(raw) != row['sha256']:
            raise ValueError('Binary differs from frozen bytes')
        source = sources[coordinate]
        if row['sha256'] not in source['frozen_binary_sha256']:
            raise ValueError('Binary is outside source-stage selection')
        if 'archive_sha256' in source:
            body = (source_root/source['relative_archive']).read_bytes()
            if sha(body) != source['archive_sha256']:
                raise ValueError('Source companion changed')
            keys = source_keys(body)
            kind = 'PUBLISHED_COMPANION'
        elif coordinate == 'androidx.datastore:datastore-preferences-external-protobuf:1.1.7':
            body = protobuf.read_bytes()
            if sha(body) != '2460a92938259e4441826e57638193d0bde9cb5cdd0dafd5367aacb833f9a971':
                raise ValueError('Mapped Protobuf sources differ')
            keys = {(p.replace('com/google/protobuf', 'androidx/datastore/preferences/protobuf'), n) for p, n in source_keys(body)}
            kind = 'PINNED_UPSTREAM_SOURCE_WITH_RECORDED_NAMESPACE_RELOCATION'
        else:
            raise ValueError('Binary lacks an inspected source companion')
        classes = archive_classes(raw)
        with safe_zip(raw) as z:
            if 'AndroidManifest.xml' in z.namelist():
                manifest = z.read('AndroidManifest.xml')
                if len(manifest) > 262144 or b'<!DOCTYPE' in manifest.upper() or b'<!ENTITY' in manifest.upper():
                    raise ValueError('Unsafe AAR manifest')
                package = ET.fromstring(manifest).attrib.get('package', '')
                if re.fullmatch('[A-Za-z0-9_.]+', package) is None:
                    raise ValueError('Invalid AAR namespace')
                namespace = package.replace('.', '/')
                if namespace in resource_namespaces:
                    raise ValueError('Ambiguous resource namespace')
                resource_namespaces[namespace] = {'coordinate': coordinate, 'r_symbols_present': 'R.txt' in z.namelist()}
        source_material[coordinate] = {'binary_sha256': row['sha256'], 'source_sha256': sha(body),
                                       'source_kind': kind, 'source_keys': len(keys), 'archive_classes': len(classes),
                                       'resource_class_name_relations': 0,
                                       'packaged_direct': 0, 'packaged_generated_relation': 0,
                                       'sourcefile_name_matches': 0, 'sourcefile_unmatched': []}
        for name, filename in classes.items():
            owners[name].append((coordinate, filename, (name.rsplit('/', 1)[0], filename) in keys))
    definitions, dex_files = [], []
    with safe_zip(raw_apk) as z:
        for x in z.infolist():
            if re.fullmatch(r'classes(?:[2-9]|[1-9][0-9]+)?\.dex', x.filename):
                raw = z.read(x)
                found, _ = dex.dex_types(raw)
                definitions.extend(d.decode('utf-8')[1:-1] for d in found)
                dex_files.append({'path': x.filename, 'sha256': sha(raw), 'classes': len(found)})
    if not definitions or len(set(definitions)) != len(definitions):
        raise ValueError('Missing or repeated APK class definitions')
    full, categories, unmatched = [], collections.Counter(), []
    for name in sorted(definitions):
        category, origin = attribution(name, owners, resource_namespaces)
        categories[category] += 1
        if origin is not None:
            matches = owners[origin]
            if len(matches) != 1:
                raise ValueError('Class name has multiple frozen owners: '+name)
            coord, filename, covered = matches[0]
            material = source_material[coord]
            material['packaged_direct' if category == 'FROZEN_CLASS_NAME' else 'packaged_generated_relation'] += 1
            if covered:
                material['sourcefile_name_matches'] += 1
            else:
                material['sourcefile_unmatched'].append({'class': name, 'origin_class': origin, 'sourcefile': filename})
            full.append({'class': name, 'category': category, 'coordinate': coord, 'origin_class': origin,
                         'sourcefile': filename, 'sourcefile_name_match': covered})
        else:
            full.append({'class': name, 'category': category})
            if category == 'AAR_NAMESPACE_RESOURCE_CLASS_NAME':
                coordinate = resource_namespaces[name.rpartition('/')[0]]['coordinate']
                source_material[coordinate]['resource_class_name_relations'] += 1
                full[-1]['coordinate'] = coordinate
            if category == 'UNATTRIBUTED_REQUIRES_REVIEW':
                unmatched.append(name)
    return {'schema': 1, 'scope': 'EXACT_APK_CLASS_NAME_AND_SOURCEFILE_MATERIAL_COVERAGE_NOT_BINARY_SOURCE_EQUIVALENCE',
            'apk_sha256': expected_apk, 'apk_bytes': len(raw_apk), 'dex_files': dex_files,
            'total_class_definitions': len(definitions), 'class_categories': dict(categories),
            'assignment_manifest_sha256': sha(json.dumps(full, sort_keys=True, separators=(',', ':')).encode()),
            'source_material': source_material, 'unattributed_classes': unmatched,
            'semantic_or_binary_source_equivalence': False, 'resources_and_native_covered_by_this_audit': False,
            'publisher_authentication_changed': False, 'complete_distribution_approved': False}


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    for name in ('apk', 'binary_plan', 'binaries', 'source_capture', 'source_root', 'protobuf', 'output'):
        p.add_argument(name, type=Path)
    for name in ('apk', 'binary-plan', 'source-capture'):
        p.add_argument('--expected-'+name, required=True)
    a = p.parse_args()
    if a.output.exists():
        raise ValueError('Audit output must be fresh')
    plan_raw, capture_raw = a.binary_plan.read_bytes(), a.source_capture.read_bytes()
    if sha(plan_raw) != a.expected_binary_plan or sha(capture_raw) != a.expected_source_capture:
        raise ValueError('Audit input pins differ')
    report = audit(a.apk, json.loads(plan_raw), a.binaries, json.loads(capture_raw), a.source_root, a.protobuf, a.expected_apk)
    report['inputs'] = {'binary_plan_sha256': sha(plan_raw), 'source_capture_sha256': sha(capture_raw),
                        'producer_sha256': sha(Path(__file__).read_bytes()),
                        'dex_parser_sha256': sha(Path(dex.__file__).read_bytes())}
    with a.output.open('x') as f:
        json.dump(report, f, indent=2)
        f.write('\n')
    print(json.dumps({'classes': report['total_class_definitions'], 'categories': report['class_categories'],
                      'sourcefile_unmatched': sum(len(x['sourcefile_unmatched']) for x in report['source_material'].values())}))
