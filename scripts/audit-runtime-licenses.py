#!/usr/bin/env python3
"""Capture declared runtime licenses and embedded notices; never grant legal or issuer approval."""
import argparse
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import hashlib
import importlib.util
import io
import json
from pathlib import Path, PurePosixPath
import re
import shutil
import threading
import time
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

spec = importlib.util.spec_from_file_location('core', Path(__file__).with_name('review-dependency-provenance.py'))
core = importlib.util.module_from_spec(spec)
spec.loader.exec_module(core)
CENTRAL = {'com.google.guava', 'com.squareup.okio', 'org.jetbrains', 'org.jetbrains.kotlin', 'org.jetbrains.kotlinx', 'org.jspecify'}


def digest(data):
    return hashlib.sha256(data).hexdigest()


def origin(group, name, version, extension):
    if any(re.fullmatch('[A-Za-z0-9_+.-]{1,200}', x) is None or x in ('.', '..') for x in (group, name, version)):
        raise ValueError('Unsafe Maven coordinate')
    if group.startswith('androidx.'):
        base = 'https://dl.google.com/dl/android/maven2/'
    elif group in CENTRAL:
        base = 'https://repo.maven.apache.org/maven2/'
    else:
        raise ValueError('Unreviewed runtime repository route')
    return base+group.replace('.', '/')+'/'+name+'/'+version+'/'+name+'-'+version+'.'+extension


def inventory(metadata, lock, metadata_pin, lock_pin):
    if digest(metadata) != metadata_pin or digest(lock) != lock_pin:
        raise ValueError('Runtime source inputs differ from their inspected pins')
    coordinates = []
    for line in lock.decode().splitlines():
        if not line or line.startswith(('#', 'empty=')):
            continue
        coordinate, configs = line.split('=', 1)
        if 'releaseRuntimeClasspath' in configs.split(','):
            if len(coordinate.split(':')) != 3 or coordinate in coordinates:
                raise ValueError('Ambiguous runtime lock selection')
            coordinates.append(coordinate)
    rows = core.captured_rows(metadata)
    result = []
    for coordinate in sorted(coordinates):
        group, name, version = coordinate.split(':')
        pom = [x for x in rows if (x['group'], x['name'], x['version']) == (group, name, version) and x['file'].endswith('.pom')]
        archives = [x for x in rows if (x['group'], x['name'], x['version']) == (group, name, version) and x['file'].endswith(('.jar', '.aar'))]
        if len(pom) > 1:
            raise ValueError('Ambiguous runtime POM')
        result.append({'coordinate': coordinate, 'pom_url': origin(group, name, version, 'pom'),
                       'pom_sha256': pom[0]['sha256'] if pom else None, 'archives': archives})
    if not result:
        raise ValueError('Empty runtime inventory')
    return result


def declarations(data):
    if len(data) > 262144 or b'<!DOCTYPE' in data.upper() or b'<!ENTITY' in data.upper():
        raise ValueError('Unsafe or oversized POM')
    root = ET.fromstring(data)
    ns = '{http://maven.apache.org/POM/4.0.0}' if root.tag.startswith('{') else ''
    if root.tag != ns+'project':
        raise ValueError('Not a Maven project descriptor')
    licenses = [{key: (node.findtext(ns+key) or '').strip() for key in ('name', 'url', 'distribution', 'comments')}
                for node in root.findall(ns+'licenses/'+ns+'license')]
    parent = root.find(ns+'parent')
    return {'licenses': licenses, 'scm_url': root.findtext(ns+'scm/'+ns+'url'),
            'parent': {k: parent.findtext(ns+k) for k in ('groupId', 'artifactId', 'version')} if parent is not None else None,
            'status': 'DECLARED_LICENSES_ONLY' if licenses else 'PARENT_OR_UPSTREAM_LICENSE_REVIEW_REQUIRED'}


def notices(data, label, output, depth=0):
    found = []
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        items = z.infolist()
        if len(items) > 20000 or len({x.filename for x in items}) != len(items) or sum(x.file_size for x in items) > 128*1024*1024:
            raise ValueError('Duplicate or oversized archive inventory')
        for item in items:
            name = PurePosixPath(item.filename)
            if name.is_absolute() or '..' in name.parts or (item.external_attr >> 16) & 0o170000 == 0o120000:
                raise ValueError('Unsafe archive member')
            upper = name.name.upper()
            if any(word in upper for word in ('LICENSE', 'LICENCE', 'NOTICE', 'COPYING', 'PATENTS')) and not upper.endswith(('.CLASS', '.SO')):
                if item.file_size > 262144:
                    raise ValueError('Oversized notice')
                raw = z.read(item)
                raw.decode('utf-8')
                sha = digest(raw)
                (output/'notices'/sha).write_bytes(raw)
                found.append({'member': label+'!/'+item.filename, 'bytes': len(raw), 'sha256': sha})
            if depth == 0 and item.filename.endswith('.jar'):
                if item.file_size > 32*1024*1024:
                    raise ValueError('Oversized nested JAR')
                found.extend(notices(z.read(item), label+'!/'+item.filename, output, 1))
    return found


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs):
        raise ValueError('Runtime evidence redirect refused')


def collect(rows, output):
    output.mkdir(parents=True, exist_ok=False)
    for name in ('poms', 'notices'):
        (output/name).mkdir()
    deadline, remaining, guard = time.monotonic()+180, [128*1024*1024], threading.Lock()

    def get(url, limit, expected=None):
        budget = deadline-time.monotonic()
        if budget <= 0:
            raise ValueError('Runtime capture deadline exhausted')
        opener = urllib.request.build_opener(NoRedirect())
        with opener.open(urllib.request.Request(url, headers={'User-Agent': 'FixMyShot-license-evidence/1'}), timeout=min(20, budget)) as response:
            if response.status != 200 or response.geturl() != url:
                raise ValueError('Runtime capture origin/status changed')
            chunks, count = [], 0
            while True:
                if time.monotonic() > deadline:
                    raise ValueError('Runtime capture time budget exceeded')
                block = response.read(min(65536, limit-count+1))
                if not block:
                    break
                count += len(block)
                with guard:
                    remaining[0] -= len(block)
                    if count > limit or remaining[0] < 0:
                        raise ValueError('Runtime capture byte budget exceeded')
                chunks.append(block)
            data = b''.join(chunks)
        if expected is not None and digest(data) != expected:
            raise ValueError('Runtime capture bytes differ from frozen inventory')
        return data

    def one(row):
        result = {'coordinate': row['coordinate'], 'pom_url': row['pom_url'], 'errors': [], 'archives': []}
        try:
            pom = get(row['pom_url'], 262144, row['pom_sha256'])
            sha = digest(pom)
            (output/'poms'/sha).write_bytes(pom)
            result.update(pom_sha256=sha, pom_bytes=len(pom), pom_matches_frozen_pin=row['pom_sha256'] is not None,
                          declaration=declarations(pom))
        except (OSError, ValueError, ET.ParseError) as error:
            result['errors'].append('POM_'+type(error).__name__)
        for archive in row['archives']:
            try:
                url = origin(archive['group'], archive['name'], archive['version'], archive['file'].rsplit('.', 1)[1])
                data = get(url, 32*1024*1024, archive['sha256'])
                result['archives'].append({'file': archive['file'], 'url': url, 'bytes': len(data), 'sha256': digest(data),
                                           'embedded_notices': notices(data, archive['file'], output)})
            except (OSError, ValueError, zipfile.BadZipFile, UnicodeError) as error:
                result['errors'].append('ARCHIVE_'+type(error).__name__+':'+archive['file'])
        return result
    with ThreadPoolExecutor(max_workers=6) as pool:
        return list(pool.map(one, rows))


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    for arg in ('metadata', 'app_lock', 'output'):
        p.add_argument(arg, type=Path)
    for arg in ('metadata', 'lock', 'source'):
        p.add_argument('--expected-'+arg, required=True)
    p.add_argument('--previous', type=Path)
    p.add_argument('--expected-previous')
    a = p.parse_args()
    if re.fullmatch('[0-9a-f]{40}', a.expected_source) is None:
        raise SystemExit('Missing inspected source identity')
    rows = inventory(core.bounded_bytes(a.metadata, 3_000_000), core.bounded_bytes(a.app_lock, 500000), a.expected_metadata, a.expected_lock)
    previous, selected = None, rows
    if a.previous is not None:
        raw = core.bounded_bytes(a.previous, 3_000_000)
        if digest(raw) != a.expected_previous:
            raise SystemExit('Previous capture differs from its inspected pin')
        previous = json.loads(raw)
        if (previous.get('scope') != 'DECLARED_RUNTIME_LICENSES_AND_EMBEDDED_TEXTS_ONLY' or
                previous.get('source_commit') != a.expected_source or previous.get('metadata_sha256') != a.expected_metadata or
                previous.get('app_lock_sha256') != a.expected_lock or
                {x['coordinate'] for x in previous['components']} != {x['coordinate'] for x in rows}):
            raise SystemExit('Previous capture belongs to another source or runtime inventory')
        missing = {x['coordinate'] for x in previous['components'] if x['errors']}
        selected = [x for x in rows if x['coordinate'] in missing]
    results = collect(selected, a.output)
    if previous is not None:
        for folder in ('poms', 'notices'):
            for file in (a.previous.parent/folder).iterdir():
                raw = core.bounded_bytes(file, 262144)
                if digest(raw) != file.name:
                    raise SystemExit('Previous cached evidence hash changed')
                (a.output/folder/file.name).write_bytes(raw)
        replacements = {x['coordinate']: x for x in results}
        results = [replacements.get(x['coordinate'], x) for x in previous['components']]
    report = {'schema': 1, 'scope': 'DECLARED_RUNTIME_LICENSES_AND_EMBEDDED_TEXTS_ONLY',
              'source_commit': a.expected_source, 'metadata_sha256': a.expected_metadata, 'app_lock_sha256': a.expected_lock,
              'reviewer_sha256': digest(Path(__file__).read_bytes()), 'captured_at_utc': datetime.now(timezone.utc).isoformat(),
              'status': 'CAPTURE_COMPLETE_REQUIRES_REVIEW' if all(not x['errors'] for x in results) else 'PARTIAL_CAPTURE_REQUIRES_REVIEW',
              'issuer_authentication_approved': False, 'full_license_compatibility_notice_source_review': False,
              'components': results}
    if previous is not None:
        report.update(previous_capture_sha256=a.expected_previous, previous_captured_at_utc=previous['captured_at_utc'],
                      network_rows_attempted=len(selected), unchanged_complete_rows_reused=len(rows)-len(selected))
    (a.output/'report.json').write_text(json.dumps(report, indent=2)+'\n')
    print(json.dumps({'status': report['status'], 'components': len(results), 'components_with_errors': sum(bool(x['errors']) for x in results)}))
