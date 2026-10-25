#!/usr/bin/env python3
"""Fresh Git restoration and source mutation/path/mode refusal; no network."""
import hashlib
import importlib.util
import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

spec=importlib.util.spec_from_file_location('kit',Path(__file__).with_name('prepare-source-kit.py'))
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


class SourceKit(unittest.TestCase):
    def setUp(self):
        temporary=tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.base=Path(temporary.name)
        self.repo=self.base/'origin'
        self.repo.mkdir()
        m.git(self.repo,'init','--quiet')
        m.git(self.repo,'config','core.autocrlf','false')
        (self.repo/'gradlew').write_bytes(b'#!/bin/sh\nexit 0\n')
        (self.repo/'gradlew').chmod(0o755)
        (self.repo/'gradlew.bat').write_bytes(b'@echo off\r\nexit /b 0\r\n')
        (self.repo/'.gitattributes').write_text('/gradlew.bat -text\n')
        m.git(self.repo,'add','--all')
        m.git(self.repo,'-c','user.name=Source fixture','-c','user.email=fixture@example.invalid',
              'commit','--quiet','-m','Source restoration fixture')
        self.commit=m.git(self.repo,'rev-parse','HEAD').decode().strip()
        self.tree=m.git(self.repo,'rev-parse','HEAD^{tree}').decode().strip()
        self.raw,self.files=m.sources(self.repo,self.commit,self.tree)
        self.record={'commit':self.commit,'tree':self.tree,'files':[
            {'path':name,**{k:v for k,v in row.items() if k!='data'}} for name,row in sorted(self.files.items())]}
        self.target=self.base/'restored'
        self.target.mkdir()
        for name,row in self.files.items():
            path=self.target/name
            path.write_bytes(row['data'])
            path.chmod(int(row['mode'],8)&0o777)

    def restore(self):
        return m.snapshot.restore(self.target,self.record,self.raw)

    def test_exact_commit_tree_pristine_state_and_crlf_recovered(self):
        self.restore()
        self.assertEqual(m.git(self.target,'rev-parse','HEAD').decode().strip(),self.commit)
        self.assertEqual(m.git(self.target,'rev-parse','HEAD^{tree}').decode().strip(),self.tree)
        self.assertEqual((self.target/'gradlew.bat').read_bytes(),b'@echo off\r\nexit /b 0\r\n')
        self.assertEqual(m.git(self.target,'status','--porcelain'),b'')

    def test_mutated_missing_and_extra_files_refused_before_git_write(self):
        path=self.target/'gradlew.bat'
        raw=path.read_bytes()
        for action in ('mutate','missing','extra'):
            if action=='mutate':path.write_bytes(b'X'+raw[1:])
            if action=='missing':path.unlink()
            if action=='extra':(self.target/'unexpected').write_text('unreviewed')
            with self.subTest(action=action),self.assertRaises(ValueError):self.restore()
            self.assertFalse((self.target/'.git').exists())
            path.write_bytes(raw)
            if (self.target/'unexpected').exists():(self.target/'unexpected').unlink()

    def test_changed_executable_mode_and_symlink_refused(self):
        path=self.target/'gradlew'
        path.chmod(0o644)
        with self.assertRaises(ValueError):self.restore()
        path.unlink()
        path.symlink_to(self.repo/'gradlew')
        with self.assertRaises(ValueError):self.restore()
        self.assertFalse((self.target/'.git').exists())

    def test_existing_git_and_changed_original_commit_refused(self):
        with self.assertRaises(ValueError):
            m.snapshot.restore(self.target,self.record,self.raw+b'changed')
        (self.target/'.git').mkdir()
        with self.assertRaises(ValueError):self.restore()

    def test_wrong_source_tree_and_noncommit_ref_refused(self):
        for commit,tree in [(self.commit,'0'*40),('HEAD',self.tree)]:
            with self.subTest(commit=commit),self.assertRaises(ValueError):m.sources(self.repo,commit,tree)

    def test_original_symlink_tree_is_not_packaged(self):
        (self.repo/'link').symlink_to('gradlew')
        m.git(self.repo,'add','link')
        m.git(self.repo,'-c','user.name=Source fixture','-c','user.email=fixture@example.invalid',
              'commit','--quiet','-m','Unsupported symlink fixture')
        commit=m.git(self.repo,'rev-parse','HEAD').decode().strip()
        tree=m.git(self.repo,'rev-parse','HEAD^{tree}').decode().strip()
        with self.assertRaises(ValueError):m.sources(self.repo,commit,tree)


if __name__=='__main__':
    unittest.main()
