#!/usr/bin/env python3
"""Review the four recorded N01 presentation differences, offline.

Keep the original 90/94 strict result. Resolve ldc width/branch positions and
synthetic parameter display only after actual class declarations match.
This is bounded instruction correspondence, never full binary equivalence.
"""
import argparse
import difflib
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import zipfile


def load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


ROOT = Path(__file__).resolve().parent
implementation = load('implementation', ROOT/'compare-repackaged-implementation.py')
symbols = load('symbols', ROOT/'compare-repackaged-symbols.py')


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def relocated_layout(value):
    if isinstance(value, tuple) and len(value) == 2 and value[0] == 1 and isinstance(value[1], str):
        return (1, implementation.mapped(bytes.fromhex(value[1]).decode()).encode().hex())
    if isinstance(value, dict):
        return {k: relocated_layout(v) for k, v in value.items()}
    if isinstance(value, (tuple, list)):
        return [relocated_layout(v) for v in value]
    return implementation.mapped(value) if isinstance(value, str) else value


def canonical(text, layout_verified=False):
    lines = text.splitlines()
    result = []
    i = 0
    while i < len(lines):
        line = lines[i]
        if layout_verified and i+1 < len(lines) and lines[i+1].startswith('descriptor: ') and '(' in line and line.endswith(';'):
            # The actual method name/flags/JVM descriptor are independently equal.
            # javap may omit synthetic constructor parameters in its pretty text.
            line = re.sub(r'\([^)]*\)', '(JVM descriptor below)', line, count=1)
        result.append(line)
        i += 1
        if line != 'Code:':
            continue
        block = []
        while i < len(lines) and lines[i]:
            block.append(lines[i])
            i += 1
        if any('Exception table:' in row for row in block):
            raise ValueError('Handler-bearing presentation changes need separate review')
        offsets = {}
        for row in block:
            match = re.fullmatch(r'(\d+):\s+([a-z][a-z0-9_]*)(.*)', row)
            if match:
                address = int(match[1])
                if address in offsets:
                    raise ValueError('Repeated instruction offset')
                offsets[address] = len(offsets)
        if not offsets or next(iter(offsets)) != 0:
            raise ValueError('Missing instruction block')
        def target(value):
            address = int(value)
            if address not in offsets:
                raise ValueError('Branch/switch target is not an instruction boundary')
            return '@'+str(offsets[address])
        for row in block:
            match = re.fullmatch(r'(\d+):\s+([a-z][a-z0-9_]*)(.*)', row)
            if match:
                address, opcode, tail = int(match[1]), match[2], match[3].strip()
                if opcode == 'ldc_w':
                    if not tail.startswith('#CP // '):
                        raise ValueError('ldc_w lacks a resolved constant')
                    opcode = 'ldc'
                if opcode.startswith('if') or opcode in ('goto', 'goto_w', 'jsr', 'jsr_w'):
                    if not re.fullmatch(r'-?\d+', tail):
                        raise ValueError('Unexpected branch operand')
                    tail = target(tail)
                result.append('@'+str(offsets[address])+' '+opcode+(' '+tail if tail else ''))
            else:
                case = re.fullmatch(r'(-?\d+|default):\s+(-?\d+)', row)
                if case:
                    result.append(case[1]+': '+target(case[2]))
                elif row == '}':
                    result.append(row)
                else:
                    raise ValueError('Unsupported instruction listing row')
    return '\n'.join(result)+'\n'


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('original_artifact_directory', type=Path)
    p.add_argument('output', type=Path)
    args = p.parse_args()
    root = args.original_artifact_directory
    manifest = (root/'SHA256SUMS.txt').read_bytes()
    if sha(manifest) != '3e7d2b90423b478b9dab18c857df81d54e33e8ff34c783cd1e5fa468c2b39986':
        raise ValueError('Original artifact manifest changed')
    expected_paths = set()
    for line in manifest.decode().splitlines():
        digest, relative = line.split('  ', 1)
        path = root/relative
        if relative in expected_paths or path.is_symlink() or not path.resolve().is_relative_to(root.resolve()) or not path.is_file():
            raise ValueError('Unsafe or repeated original evidence path')
        if sha(path.read_bytes()) != digest:
            raise ValueError('Original artifact member changed: '+relative)
        expected_paths.add(relative)
    actual_paths = {str(path.relative_to(root)) for path in root.rglob('*') if path.is_file() and path.name != 'SHA256SUMS.txt'}
    if len(expected_paths) != 229 or actual_paths != expected_paths:
        raise ValueError('Original artifact membership changed')
    report_raw = (root/'comparison.json').read_bytes()
    if sha(report_raw) != '67fd0852ba901cca41aa55e44aa67ef6d5078efbe1d518b055f9f3e03a99728b':
        raise ValueError('This review is bound to the inspected original N01 report')
    report = json.loads(report_raw)
    for row in report['inputs']:
        if sha((root/row['file']).read_bytes()) != row['sha256']:
            raise ValueError('Original N01 input changed')
    expected_differences = {
        ('published_to_frozen', 'com/google/flatbuffers/FlatBufferBuilder'),
        ('published_to_frozen', 'com/google/flatbuffers/FlexBuffersBuilder'),
        ('compiled_to_frozen', 'com/google/flatbuffers/FlatBufferBuilder'),
        ('compiled_to_frozen', 'com/google/flatbuffers/Table$1'),
    }
    raw = {}
    for label, jar in (('published', 'flatbuffers.jar'), ('frozen', 'frozen.jar')):
        with zipfile.ZipFile(root/jar) as archive:
            for name in implementation.classes((root/jar).read_bytes()):
                raw[label, name] = archive.read(name+'.class')
    for path in (root/'compiled').rglob('*.class'):
        raw['compiled', str(path.relative_to(root/'compiled'))[:-6]] = path.read_bytes()
    checked, actual_differences = [], set()
    for row in report['rows']:
        label = row['comparison'].split('_to_')[0]
        name, target = row['original'], row['frozen']
        left, right = raw[label, name], raw['frozen', target]
        same_layout = relocated_layout(symbols.symbols(left)['layout']) == relocated_layout(symbols.symbols(right)['layout'])
        if not same_layout:
            raise ValueError('Actual class declarations differ: '+name)
        lb, rb = implementation.bootstrap_methods(left), implementation.bootstrap_methods(right)
        a = (root/'disassembly'/label/(name+'.txt')).read_text()
        b = (root/'disassembly/frozen'/(target+'.txt')).read_text()
        strict = implementation.compare(a, b, lb, rb)
        if json.dumps(strict, sort_keys=True) != json.dumps({k: row[k] for k in strict}, sort_keys=True):
            raise ValueError('Original comparison does not replay')
        if strict['normalized_disassembly_identical']:
            identical = True
        else:
            key = (row['comparison'], name)
            if key not in expected_differences:
                raise ValueError('Unexpected presentation difference')
            actual_differences.add(key)
            ca = canonical(implementation.normalize(a, bool(lb)), layout_verified=True)
            cb = canonical(implementation.normalize(b, bool(rb)), layout_verified=True)
            identical = ca == cb and lb == rb
            if not identical:
                print(''.join(difflib.unified_diff(ca.splitlines(True), cb.splitlines(True), fromfile=label+'/'+name, tofile='frozen/'+target)))
        checked.append({'comparison': row['comparison'], 'original': name, 'frozen': target,
                        'actual_declarations_identical': same_layout,
                        'strict_disassembly_identical': strict['normalized_disassembly_identical'],
                        'bounded_instruction_correspondence': identical,
                        'bootstrap_methods_identical': lb == rb,
                        'original_class_sha256': sha(left), 'frozen_class_sha256': sha(right)})
    if actual_differences != expected_differences or len(checked) != 94:
        raise ValueError('N01 comparison coverage changed')
    result = {
        'schema': 1, 'scope': 'EXACT_N01_BOUNDED_IMPLEMENTATION_CORRESPONDENCE',
        'status': 'PASS' if all(r['bounded_instruction_correspondence'] for r in checked) else 'HOLD',
        'original_report_sha256': sha(report_raw), 'strict_matches_retained': 90,
        'presentation_differences_reviewed': 4, 'comparisons': 94, 'rows': checked,
        'compiled_source_classes_compared_to_all_frozen_classes': 49,
        'compiled_noto_generated_classes_matching_strictly': 4,
        'exact_historical_noto_build_checkout_proved': False,
        'class_bytes_debug_annotations_and_all_attributes_equivalent': False,
        'publisher_assurance_changed': False, 'whole_release_approved': False,
        'reviewer_sha256': sha(Path(__file__).read_bytes()),
    }
    with args.output.open('x') as stream:
        json.dump(result, stream, indent=2)
        stream.write('\n')
    print(json.dumps({k: result[k] for k in ('status', 'comparisons', 'strict_matches_retained', 'presentation_differences_reviewed')}))
    if result['status'] != 'PASS':
        raise SystemExit(1)


if __name__ == '__main__':
    main()

