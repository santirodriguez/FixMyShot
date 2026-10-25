#!/usr/bin/env python3
"""Restore exact public Git identities in a fresh, verified source kit; no network."""
import hashlib
import json
from pathlib import Path
import re
import subprocess


def digest(data,kind='blob'):
    return hashlib.sha1(kind.encode()+b' '+str(len(data)).encode()+b'\0'+data).hexdigest()


def git(root,*args,data=None):
    return subprocess.check_output(['git','-C',str(root),*args],input=data).decode().strip()


def restore(root,record,commit_bytes):
    if (root/'.git').exists() or root.is_symlink():
        raise ValueError('Restore requires a fresh source directory without .git')
    if digest(commit_bytes,'commit')!=record['commit'] or not commit_bytes.startswith(('tree '+record['tree']+'\n').encode()):
        raise ValueError('Original public Git commit does not match its explicit identity')
    expected={row['path']:row for row in record['files']}
    if len(expected)!=len(record['files']):
        raise ValueError('Duplicate source path')
    actual={str(p.relative_to(root)) for p in root.rglob('*') if not p.is_dir()}
    if actual!=set(expected):
        raise ValueError('Missing or additional source files')
    rows=[]
    for name,row in sorted(expected.items()):
        path=root/name
        if (Path(name).is_absolute() or '..' in Path(name).parts or path.is_symlink() or
                row['mode'] not in ('100644','100755')):
            raise ValueError('Unsafe source path or mode')
        data=path.read_bytes()
        if (len(data)!=row['bytes'] or hashlib.sha256(data).hexdigest()!=row['sha256'] or
                digest(data)!=row['blob_sha1'] or bool(path.stat().st_mode&0o111)!=(row['mode']=='100755')):
            raise ValueError('Source bytes or executable mode changed: '+name)
        rows.append((name,row,data))
    git(root,'init','--quiet')
    git(root,'config','core.autocrlf','false')
    # Write original bytes without checkout filters, preserving wrapper BAT CRLF.
    index=[]
    for name,row,data in rows:
        oid=git(root,'hash-object','-w','--stdin',data=data)
        index.append((row['mode']+' '+oid+'\t'+name+'\0').encode())
    git(root,'update-index','-z','--index-info',data=b''.join(index))
    if git(root,'write-tree')!=record['tree']:
        raise ValueError('Restored Git tree differs from the accepted source')
    git(root,'hash-object','-t','commit','-w','--stdin',data=commit_bytes)
    (root/'.git/shallow').write_text(record['commit']+'\n')
    git(root,'symbolic-ref','HEAD','refs/heads/source-snapshot')
    git(root,'update-ref','HEAD',record['commit'])
    if git(root,'status','--porcelain','--untracked-files=all'):
        raise ValueError('Restored source is not pristine')
    return {'commit':record['commit'],'tree':record['tree'],'files':len(rows)}


if __name__=='__main__':
    root=Path(__file__).resolve().parent
    manifest=json.loads((root/'PINNED-SOURCES.json').read_text())
    if manifest.get('schema')!=1:
        raise ValueError('Unexpected source-kit schema')
    allowed={'project':'project','webp':'upstream/libwebp'}
    if set(manifest['repositories'])!=set(allowed):
        raise ValueError('Unexpected source-kit repositories')
    for name,record in manifest['repositories'].items():
        target=root/record['directory']
        if record['directory']!=allowed[name] or not target.resolve().is_relative_to(root):
            raise ValueError('Unsafe repository directory')
        print(json.dumps(restore(target,record,(root/'source-control'/f'{name}.commit').read_bytes())))
