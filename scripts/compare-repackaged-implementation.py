#!/usr/bin/env python3
"""Collect an isolated N01 instruction comparison; never approve a release."""
import argparse
from concurrent.futures import ThreadPoolExecutor
import difflib
import hashlib
import importlib.util
import io
import json
from pathlib import Path, PurePosixPath
import re
import subprocess
import zipfile

MAPPINGS = (('com/google/flatbuffers', 'androidx/emoji2/text/flatbuffer'),
            ('com.google.flatbuffers', 'androidx.emoji2.text.flatbuffer'),
            ('androidx/text/emoji/flatbuffer', 'androidx/emoji2/text/flatbuffer'),
            ('androidx.text.emoji.flatbuffer', 'androidx.emoji2.text.flatbuffer'))
CP_OPS = {'ldc', 'ldc_w', 'ldc2_w', 'getstatic', 'putstatic', 'getfield',
          'putfield', 'invokevirtual', 'invokespecial', 'invokestatic',
          'invokeinterface', 'new', 'anewarray', 'checkcast', 'instanceof',
          'multianewarray', 'invokedynamic'}


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def mapped(text):
    for before, after in MAPPINGS:
        text = text.replace(before, after)
    return text


def normalize(text, bootstrap_verified=False):
    """Only declared relocation, resolved operand indices and SourceFile heading.
    Preserve offsets, opcodes, literals, handlers, members and method order.
    This intentionally reports differences when compiler layout changes.
    """
    lines = []
    for line in text.splitlines():
        if line.startswith('Compiled from "'):
            continue
        match = re.fullmatch(r'(\s*\d+:\s+)([a-z][a-z0-9_]*)(.*)', line)
        if match:
            prefix, opcode, operand = match.groups()
            if (opcode == 'invokedynamic' or '// Dynamic ' in operand) and not bootstrap_verified:
                raise ValueError('Dynamic bootstrap comparison requires resolved attributes')
            if '#' in operand.split('//', 1)[0]:
                if opcode not in CP_OPS or '//' not in operand:
                    raise ValueError('Unresolved or unsupported constant operand')
                value, comment = operand.split('//', 1)
                if not comment.strip() or not re.search(r'#\d+', value):
                    raise ValueError('Missing resolved operand')
                value = re.sub(r'#\d+', '#CP', value, count=1)
                line = prefix + opcode + ' ' + value.strip() + ' // ' + comment.strip()
        lines.append(mapped(line.strip()))
    return '\n'.join(lines).strip() + '\n'


def compare(left, right, left_bootstrap=None, right_bootstrap=None):
    a = normalize(left, bool(left_bootstrap))
    b = normalize(right, bool(right_bootstrap))
    return {
        'normalized_disassembly_identical': a == b and left_bootstrap == right_bootstrap,
        'bootstrap_methods_identical': left_bootstrap == right_bootstrap,
        'original_bootstrap': left_bootstrap, 'frozen_bootstrap': right_bootstrap,
        'original_normalized_sha256': sha(a.encode()),
        'frozen_normalized_sha256': sha(b.encode()),
        'instructions_and_exception_tables_compared': True,
        'byte_offsets_and_opcodes_preserved': True,
        'debug_annotations_other_attributes_compared': False,
        'formal_semantic_or_full_binary_source_equivalence': False,
    }



def bootstrap_methods(raw):
    """Resolve actual BootstrapMethods, including method handles and arguments.
    Fail closed on recursive/dynamic constants; no external implementation runs.
    """
    import struct
    pos = 8
    if len(raw) < 10 or raw[:4] != b'\xca\xfe\xba\xbe':
        raise ValueError('Invalid bootstrap class')
    def take(n):
        nonlocal pos
        if n < 0 or pos+n > len(raw):
            raise ValueError('Truncated bootstrap class')
        value = raw[pos:pos+n]
        pos += n
        return value
    def u2():
        return struct.unpack('>H', take(2))[0]
    def u4():
        return struct.unpack('>I', take(4))[0]
    cp = [None]*u2()
    i = 1
    while i < len(cp):
        tag = take(1)[0]
        if tag == 1:
            cp[i] = (tag, mapped(take(u2()).decode('utf-8')))
        elif tag in (3, 4, 5, 6):
            cp[i] = (tag, take(8 if tag in (5, 6) else 4).hex())
        elif tag in (7, 8, 16, 19, 20):
            cp[i] = (tag, u2())
        elif tag in (9, 10, 11, 12, 17, 18):
            cp[i] = (tag, u2(), u2())
        elif tag == 15:
            cp[i] = (tag, take(1)[0], u2())
        else:
            raise ValueError('Unsupported bootstrap constant')
        i += 2 if tag in (5, 6) else 1
    def resolve(index, stack=()):
        if not 0 < index < len(cp) or cp[index] is None or index in stack or len(stack) > 24:
            raise ValueError('Invalid bootstrap reference')
        tag, *values = cp[index]
        if tag in (1, 3, 4, 5, 6):
            return (tag, *values)
        if tag == 15:
            if not 1 <= values[0] <= 9:
                raise ValueError('Invalid method handle kind')
            return (tag, values[0], resolve(values[1], stack+(index,)))
        if tag in (17, 18):
            raise ValueError('Dynamic bootstrap argument needs separate resolution')
        return (tag, *(resolve(v, stack+(index,)) for v in values))
    def attrs():
        entries = []
        for _ in range(u2()):
            name = resolve(u2())
            if name[0] != 1:
                raise ValueError('Non-UTF8 attribute name')
            entries.append((name[1], take(u4())))
        return entries
    take(6)
    take(u2()*2)
    for _ in range(2):
        for _ in range(u2()):
            take(6)
            attrs()
    entries = [value for name, value in attrs() if name == 'BootstrapMethods']
    if pos != len(raw) or len(entries) > 1:
        raise ValueError('Trailing class or duplicate bootstrap attribute')
    if not entries:
        return []
    data = entries[0]
    offset = 0
    def read():
        nonlocal offset
        if offset+2 > len(data):
            raise ValueError('Truncated BootstrapMethods')
        value = struct.unpack_from('>H', data, offset)[0]
        offset += 2
        return value
    rows = []
    for _ in range(read()):
        method = resolve(read())
        if method[0] != 15:
            raise ValueError('Bootstrap target is not a method handle')
        rows.append({'method': method, 'arguments': [resolve(read()) for _ in range(read())]})
    if offset != len(data):
        raise ValueError('Trailing BootstrapMethods bytes')
    return rows


def classes(raw):
    with zipfile.ZipFile(io.BytesIO(raw)) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)) or len(names) > 10000:
            raise ValueError('Ambiguous class archive')
        return sorted(n[:-6] for n in names if n.endswith('.class'))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('output', type=Path)
    parser.add_argument('--jdk', type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    spec = importlib.util.spec_from_file_location('capture', root/'scripts/capture-runtime-sources.py')
    capture = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(capture)
    import urllib.request
    flat = root/'docs/feasibility/evidence/runtime-source-closure-e516aa89/flatbuffers'
    inputs = [
        ('flatbuffers.jar', 'https://repo.maven.apache.org/maven2/com/google/flatbuffers/flatbuffers-java/1.12.0/flatbuffers-java-1.12.0.jar', '3f8c088b4dd04a9858721f2e162508c94db0dd86f961e306ee63ef2eda871bf7'),
        ('flatbuffers-sources.jar', 'https://repo.maven.apache.org/maven2/com/google/flatbuffers/flatbuffers-java/1.12.0/flatbuffers-java-1.12.0-sources.jar', '6522155eaadf630f60c05154c4f79505f14675e408d10e0f40c7fbfebf0a21ab'),
        ('emoji2.aar', 'https://dl.google.com/dl/android/maven2/androidx/emoji2/emoji2/1.4.0/emoji2-1.4.0.aar', '433febd3434a45667176c76a64f3f205ca6335a6b544c5b5d57f25a38a375242'),
    ]
    receipts = []
    for name, url, expected in inputs:
        capture.checked_url(url)
        opener = urllib.request.build_opener(capture.Redirect())
        with opener.open(urllib.request.Request(url, headers={'User-Agent': 'FixMyShot-N01/1'}), timeout=30) as response:
            raw = response.read(1024*1024 + 1)
        if len(raw) > 1024*1024 or sha(raw) != expected:
            raise ValueError('N01 input differs from frozen hash: '+name)
        capture.inspect(raw)
        (out/name).write_bytes(raw)
        receipts.append({'file': name, 'url': url, 'bytes': len(raw), 'sha256': expected})
    with zipfile.ZipFile(out/'emoji2.aar') as archive:
        if archive.namelist().count('libs/repackaged.jar') != 1:
            raise ValueError('Missing or ambiguous frozen implementation')
        frozen = archive.read('libs/repackaged.jar')
        capture.inspect(frozen)
        (out/'frozen.jar').write_bytes(frozen)
    source_rows = json.loads((flat/'published-source-files.json').read_text())['files']
    if len(source_rows) != 24:
        raise ValueError('Unexpected FlatBuffers source set')
    source_paths, source_receipts = [], []
    src = out/'java-source'
    with zipfile.ZipFile(out/'flatbuffers-sources.jar') as archive:
        for row in source_rows:
            raw = (flat/row['local_file']).read_bytes()
            if sha(raw) != row['sha256'] or raw != archive.read(row['original_path']):
                raise ValueError('Staged source differs from original source companion')
            path = src/row['original_path']
            if '..' in PurePosixPath(row['original_path']).parts or path.is_absolute() and not path.is_relative_to(src):
                raise ValueError('Invalid source path')
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(raw)
            source_paths.append(path)
            source_receipts.append({'path': row['original_path'], 'bytes': len(raw), 'sha256': sha(raw)})
    for name, expected in (
            ('MetadataItem', '035efd4dd11a794355eddb6c7e884428a6cbcd26cb8b1d9bf8eeae7987213370'),
            ('MetadataList', '2c28c16002e75ce439ebc88202033ee8bbb5fec7cff8efad5a94c1a64f707f33')):
        raw = (flat/('tag-emoji-compat-flatbuffers__src__java__androidx__text__emoji__flatbuffer__'+name+'.java')).read_bytes()
        if sha(raw) != expected:
            raise ValueError('Noto source differs from recorded historical material')
        path = src/('androidx/text/emoji/flatbuffer/'+name+'.java')
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(raw)
        source_paths.append(path)
        source_receipts.append({'path': str(path.relative_to(src)), 'bytes': len(raw), 'sha256': expected})
    javac, javap = args.jdk/'bin/javac', args.jdk/'bin/javap'
    version = subprocess.run([str(javac), '-version'], capture_output=True, text=True, check=True, timeout=10)
    if not version.stdout.startswith('javac 17.'):
        raise ValueError('N01 requires the existing JDK17 toolchain')
    (out/'javac-version.txt').write_text(version.stdout+version.stderr)
    built = out/'compiled'
    result = subprocess.run([str(javac), '--release', '8', '-proc:none', '-encoding', 'UTF-8',
                             '-g', '-d', str(built), *map(str, source_paths)],
                            capture_output=True, text=True, timeout=90)
    (out/'javac.txt').write_text(result.stdout+result.stderr)
    result.check_returncode()
    frozen_names = classes(frozen)
    original_names = classes((out/'flatbuffers.jar').read_bytes())
    compiled_names = sorted(str(p.relative_to(built))[:-6] for p in built.rglob('*.class'))
    if len(frozen_names) != 49 or len(original_names) != 45 or len(compiled_names) != 49:
        raise ValueError('Unexpected class coverage')
    if set(map(mapped, compiled_names)) != set(frozen_names):
        raise ValueError('Compiled material does not cover the exact frozen class set')
    jobs = []
    for label, cp, names in (('published', out/'flatbuffers.jar', original_names),
                              ('frozen', out/'frozen.jar', frozen_names),
                              ('compiled', built, compiled_names)):
        for name in names:
            jobs.append((label, cp, name))
    def disassemble(job):
        label, cp, name = job
        result = subprocess.run([str(javap), '-classpath', str(cp), '-c', '-p', '-s', '-constants', name.replace('/', '.')],
                                capture_output=True, text=True, check=True, timeout=30)
        if result.stderr or not result.stdout.strip():
            raise ValueError('Unexpected disassembler diagnostic')
        path = out/'disassembly'/label/(name+'.txt')
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(result.stdout)
        return (label, name), result.stdout
    with ThreadPoolExecutor(max_workers=4) as pool:
        dumps = dict(pool.map(disassemble, jobs))
    raw_classes = {}
    for label, jar in (('published', out/'flatbuffers.jar'), ('frozen', out/'frozen.jar')):
        with zipfile.ZipFile(jar) as archive:
            for name in classes(jar.read_bytes()):
                raw_classes[label, name] = archive.read(name+'.class')
    for name in compiled_names:
        raw_classes['compiled', name] = (built/(name+'.class')).read_bytes()
    rows = []
    for label, names in (('published', original_names), ('compiled', compiled_names)):
        for name in names:
            target = mapped(name)
            left, right = dumps[label, name], dumps['frozen', target]
            row = {'comparison': label+'_to_frozen', 'original': name, 'frozen': target, **compare(left, right, bootstrap_methods(raw_classes[label, name]), bootstrap_methods(raw_classes['frozen', target]))}
            rows.append(row)
            if not row['normalized_disassembly_identical']:
                path = out/'differences'/label/(name+'.diff')
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(''.join(difflib.unified_diff(normalize(left, bool(row['original_bootstrap'])).splitlines(True), normalize(right, bool(row['frozen_bootstrap'])).splitlines(True),
                                                           fromfile=label+'/'+name, tofile='frozen/'+target)))
    report = {
        'schema': 1, 'scope': 'N01_INSTRUCTION_COMPARISON_WITH_EXPLICIT_RELOCATION_AND_RESOLVED_CP_INDICES_ONLY',
        'status': 'ALL_DISASSEMBLIES_MATCH' if all(r['normalized_disassembly_identical'] for r in rows) else 'DIFFERENCES_REQUIRE_REVIEW',
        'compiled_application_source': '7cbed17de44f4bc4ff31b62270ace3bd5a51b492',
        'producer_sha256': sha(Path(__file__).read_bytes()), 'inputs': receipts,
        'original_source_files': source_receipts, 'rows': rows,
        'published_classes': 45, 'compiled_source_classes': 49, 'frozen_classes': 49,
        'exact_historical_noto_build_checkout_proved': False,
        'bootstrap_methods_and_arguments_compared': True,
        'annotations_debug_other_attributes_compared': False,
        'formal_semantic_equivalence_or_final_source_clearance': False,
        'new_android_build_phone_test_signing_or_publication': False,
    }
    (out/'comparison.json').write_text(json.dumps(report, indent=2)+'\n')
    (out/'SHA256SUMS.txt').write_text(''.join(sha(p.read_bytes())+'  '+str(p.relative_to(out))+'\n'
                                             for p in sorted(out.rglob('*')) if p.is_file()))
    print(json.dumps({'status': report['status'], 'compared': len(rows),
                      'identical': sum(r['normalized_disassembly_identical'] for r in rows)}))


if __name__ == '__main__':
    main()
