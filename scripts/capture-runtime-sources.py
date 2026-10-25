#!/usr/bin/env python3
"""Stage published source companions; never approve artifact identity or legal completeness."""
import argparse
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import hashlib
import importlib.util
import io
import json
from pathlib import Path, PurePosixPath
import stat
import threading
import time
import urllib.parse
import urllib.request
import zipfile

spec = importlib.util.spec_from_file_location('runtime_license_capture', Path(__file__).with_name('audit-runtime-licenses.py'))
runtime = importlib.util.module_from_spec(spec)
spec.loader.exec_module(runtime)
MAX_ARCHIVE = 16 * 1024 * 1024
MAX_TOTAL = 128 * 1024 * 1024
MAX_EXPANDED = 256 * 1024 * 1024
HOSTS = {'dl.google.com', 'repo.maven.apache.org'}
KNOWN_EMPTY = 'androidx.datastore:datastore-preferences-external-protobuf:1.1.7'


def digest(data):
    return hashlib.sha256(data).hexdigest()


def checked_url(url):
    u = urllib.parse.urlsplit(url)
    if u.scheme != 'https' or u.hostname not in HOSTS or u.username or u.password or u.port or u.query or u.fragment:
        raise ValueError('Unapproved source repository URL')
    return url


class Redirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        checked_url(newurl)
        return super().redirect_request(req, fp, code, msg, headers, newurl)


def inspect(data):
    if len(data) > MAX_ARCHIVE:
        raise ValueError('Source archive compressed limit')
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        items = z.infolist()
        if len(items) > 20000 or len({x.filename for x in items}) != len(items):
            raise ValueError('Duplicate or excessive source members')
        expanded = sum(x.file_size for x in items)
        if expanded > MAX_EXPANDED:
            raise ValueError('Source archive expansion limit')
        files, source_count, source_bytes, notices = [], 0, 0, []
        for x in items:
            p = PurePosixPath(x.filename)
            mode = x.external_attr >> 16
            if not x.filename or '\\' in x.filename or ':' in x.filename or p.is_absolute() or '..' in p.parts or stat.S_ISLNK(mode) or x.flag_bits & 1:
                raise ValueError('Unsafe source archive member')
            if not x.is_dir() and stat.S_IFMT(mode) not in (0, stat.S_IFREG):
                raise ValueError('Nonregular source archive member')
            if x.is_dir():
                continue
            raw = z.read(x)  # CRC is checked; nothing is extracted or executed.
            files.append({'path': x.filename, 'bytes': len(raw), 'sha256': digest(raw)})
            if p.suffix.lower() in ('.java', '.kt', '.kts', '.c', '.h', '.cc', '.cpp', '.hpp'):
                source_count += 1
                source_bytes += len(raw)
            if any(t in p.name.upper() for t in ('LICENSE', 'LICENCE', 'NOTICE', 'COPYING', 'PATENTS')):
                notices.append(x.filename)
        manifest = json.dumps(sorted(files, key=lambda f: f['path']), sort_keys=True, separators=(',', ':')).encode()
        return {'members': len(items), 'regular_files': len(files), 'expanded_bytes': expanded,
                'source_files': source_count, 'source_bytes': source_bytes,
                'source_presence': 'SOURCE_TEXT_PRESENT_NOT_COMPLETENESS_PROOF' if source_count else 'NO_SOURCE_TEXT_PAYLOAD_REQUIRES_UPSTREAM_REVIEW',
                'notice_paths': notices, 'member_manifest_sha256': digest(manifest)}


def plan(metadata, lock, catalog, metadata_pin, lock_pin, catalog_pin):
    if digest(catalog) != catalog_pin:
        raise ValueError('Runtime catalog pin mismatch')
    declared = json.loads(catalog)
    rows = runtime.inventory(metadata, lock, metadata_pin, lock_pin)
    coords = [x['coordinate'] for x in declared['components']]
    if len(set(coords)) != len(coords) or set(coords) != {x['coordinate'] for x in rows}:
        raise ValueError('Runtime catalog differs from selected inventory')
    for i, row in enumerate(rows):
        row['frozen_binary_sha256'] = [x['sha256'] for x in row.pop('archives')]
        if row['coordinate'] == KNOWN_EMPTY:
            row.update(status='KNOWN_EMPTY_COMPANION_NOT_RETRIED_USE_RECORDED_UPSTREAM_MAPPING',
                       prior_review='docs/feasibility/evidence/runtime-license-review-f9434ab9.json#protobuf_sources',
                       empty_companion_sha256='c6deada2fac53b8ea6523dbda77597b128006674616f140f04df23264c6d1aa3')
        elif row['frozen_binary_sha256']:
            g, n, v = row['coordinate'].split(':')
            row.update(source_url=checked_url(runtime.origin(g, n, v, 'jar')[:-4]+'-sources.jar'),
                       relative_archive=f'archives/{i:03d}-{n}-{v}-sources.jar')
        else:
            row['status'] = 'NO_BINARY_CAPTURE_FOR_COORDINATE_NOT_AUTOMATIC_SOURCE_EXEMPTION'
    return rows


def capture(rows, output):
    output.mkdir(parents=True, exist_ok=False)
    (output/'archives').mkdir()
    deadline, used, expanded, mutex = time.monotonic()+120, 0, 0, threading.Lock()

    def one(row):
        nonlocal used, expanded
        if 'source_url' not in row:
            return row
        result = dict(row)
        try:
            req = urllib.request.Request(checked_url(row['source_url']), headers={'User-Agent': 'FixMyShot-source-staging/1'})
            opener = urllib.request.build_opener(Redirect())
            remaining = deadline-time.monotonic()
            if remaining <= 0:
                raise ValueError('Source capture deadline')
            with mutex:
                if used >= MAX_TOTAL or expanded >= MAX_EXPANDED:
                    raise ValueError('Source capture aggregate budget exhausted')
            with opener.open(req, timeout=min(10, remaining)) as response:
                checked_url(response.url)
                data = bytearray()
                while True:
                    if time.monotonic() >= deadline:
                        raise ValueError('Source capture deadline')
                    block = response.read(65536)
                    if not block:
                        break
                    if len(data)+len(block) > MAX_ARCHIVE:
                        raise ValueError('Source archive compressed limit')
                    with mutex:
                        used += len(block)
                        if used > MAX_TOTAL:
                            raise ValueError('Source capture aggregate limit')
                    data.extend(block)
            raw = bytes(data)
            summary = inspect(raw)
            with mutex:
                expanded += summary['expanded_bytes']
                if expanded > MAX_EXPANDED:
                    raise ValueError('Source capture aggregate expansion limit')
            (output/row['relative_archive']).write_bytes(raw)
            result.update(status='CAPTURED_REQUIRES_SOURCE_REVIEW', archive_bytes=len(raw), archive_sha256=digest(raw), **summary)
        except Exception as e:
            result.update(status='CAPTURE_FAILED_NOT_RETRIED', error=type(e).__name__+': '+str(e))
        return result

    with ThreadPoolExecutor(max_workers=6) as pool:
        return list(pool.map(one, rows))


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('metadata', type=Path)
    p.add_argument('lock', type=Path)
    p.add_argument('catalog', type=Path)
    p.add_argument('output', type=Path)
    for name in ('metadata', 'lock', 'catalog'):
        p.add_argument('--expected-'+name, required=True)
    a = p.parse_args()
    rows = plan(a.metadata.read_bytes(), a.lock.read_bytes(), a.catalog.read_bytes(), a.expected_metadata, a.expected_lock, a.expected_catalog)
    results = capture(rows, a.output)
    scripts = ('capture-runtime-sources.py', 'audit-runtime-licenses.py', 'review-dependency-provenance.py')
    report = {'schema': 1, 'scope': 'PUBLISHED_SOURCE_COMPANION_STAGING_NOT_CORRESPONDING_SOURCE_OR_ISSUER_APPROVAL',
              'captured_at_utc': datetime.now(timezone.utc).isoformat(),
              'metadata_sha256': a.expected_metadata, 'app_lock_sha256': a.expected_lock, 'catalog_sha256': a.expected_catalog,
              'producer_files': {s: digest(Path(__file__).with_name(s).read_bytes()) for s in scripts},
              'issuer_authentication_approved': False, 'complete_corresponding_source_approved': False,
              'components': results}
    (a.output/'report.json').write_text(json.dumps(report, indent=2)+'\n')
    print(json.dumps({'components': len(results), 'captured': sum(x['status']=='CAPTURED_REQUIRES_SOURCE_REVIEW' for x in results),
                      'without_frozen_binary': sum(not x['frozen_binary_sha256'] for x in results),
                      'known_empty_not_retried': sum(x['coordinate']==KNOWN_EMPTY for x in results),
                      'failed': sum(x['status']=='CAPTURE_FAILED_NOT_RETRIED' for x in results)}))


if __name__ == '__main__':
    main()
