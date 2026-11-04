#!/usr/bin/env python3
"""Require identical APK entries and packaging except one exact source-revision record.

This is cross-source payload comparison, never a byte-identical rebuild verdict.
"""
import argparse
import hashlib
import io
import json
from pathlib import Path
import re
import stat
import zipfile

RECORD = 'META-INF/version-control-info.textproto'
MAX_APK = 128*1024*1024


def revision(commit):
    if not re.fullmatch('[0-9a-f]{40}',commit):
        raise ValueError('Expected a full source commit')
    return ('repositories {\n  system: GIT\n  local_root_path: "$PROJECT_DIR"\n  revision: "'+commit+'"\n}\n').encode()


def compare(old_bytes,new_bytes,old_commit,new_commit):
    old_record,new_record = revision(old_commit),revision(new_commit)
    if old_commit == new_commit or any(len(x)>MAX_APK for x in (old_bytes,new_bytes)):
        raise ValueError('Need bounded APKs from two distinct explicit source commits')
    if len(old_bytes)!=len(new_bytes):
        raise ValueError('APK container size changed')
    with zipfile.ZipFile(io.BytesIO(old_bytes)) as old,zipfile.ZipFile(io.BytesIO(new_bytes)) as new:
        for archive in (old,new):
            infos = archive.infolist()
            if (len(infos)>10000 or len(infos)!=len(set(x.filename for x in infos)) or
                    sum(x.file_size for x in infos)>MAX_APK or archive.testzip() is not None or
                    any(x.filename.startswith('/') or '..' in Path(x.filename).parts or
                        x.flag_bits&1 or stat.S_ISLNK(x.external_attr>>16) for x in infos)):
                raise ValueError('Unsafe, duplicate, oversized or corrupt APK members')
        if old.namelist()!=new.namelist() or old.comment!=new.comment or RECORD not in old.namelist():
            raise ValueError('APK member set/order/comment or revision record changed')
        unchanged=[]
        moved=[]
        for name in old.namelist():
            left,right=old.getinfo(name),new.getinfo(name)
            attrs=('date_time','compress_type','flag_bits','external_attr','create_system',
                   'extra','comment','file_size')
            if any(getattr(left,key)!=getattr(right,key) for key in attrs):
                raise ValueError('APK entry packaging changed: '+name)
            if left.header_offset!=right.header_offset:
                moved.append({'path':name,'old_offset':left.header_offset,'new_offset':right.header_offset})
            before,after=old.read(name),new.read(name)
            if name==RECORD:
                if before!=old_record or after!=new_record:
                    raise ValueError('Source revision record differs from explicit expected sources')
            elif before!=after or left.compress_size!=right.compress_size:
                raise ValueError('Application payload changed: '+name)
            else:
                unchanged.append({'path':name,'bytes':len(after),'sha256':hashlib.sha256(after).hexdigest()})
    return {'schema':1,'scope':'CROSS_SOURCE_APK_PAYLOAD_COMPARISON_NOT_BYTE_IDENTICAL_REBUILD',
            'old_source_commit':old_commit,'new_source_commit':new_commit,
            'old_apk_sha256':hashlib.sha256(old_bytes).hexdigest(),
            'new_apk_sha256':hashlib.sha256(new_bytes).hexdigest(),'apk_bytes':len(new_bytes),
            'byte_identical':old_bytes==new_bytes,'only_changed_entry':RECORD,
            'expected_source_records_verified':True,'entry_metadata_unchanged':True,
            'source_record_compressed_bytes':{'old':old.getinfo(RECORD).compress_size,'new':new.getinfo(RECORD).compress_size},
            'relocated_entry_headers':moved,'native_alignment_requires_separate_check':True,
            'identical_payload_entries':len(unchanged),'payloads':unchanged,
            'publisher_authentication':'OPEN','physical_acceptance':'NOT_RUN'}


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ('old_apk','new_apk','output'):
        parser.add_argument(name,type=Path)
    parser.add_argument('--old-source',required=True)
    parser.add_argument('--new-source',required=True)
    args=parser.parse_args()
    for path in (args.old_apk,args.new_apk):
        if not path.is_file() or path.is_symlink() or path.stat().st_size>MAX_APK:
            raise ValueError('Invalid or oversized APK input')
    report=compare(args.old_apk.read_bytes(),args.new_apk.read_bytes(),args.old_source,args.new_source)
    with args.output.open('x') as stream:
        json.dump(report,stream,indent=2)
        stream.write('\n')
    print(json.dumps({key:report[key] for key in ('byte_identical','identical_payload_entries','only_changed_entry')}))
