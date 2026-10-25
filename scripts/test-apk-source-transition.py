#!/usr/bin/env python3
"""Reject code, source-record, packaging and duplicate-entry changes."""
import importlib.util
import io
from pathlib import Path
import unittest
import warnings
import zipfile

spec=importlib.util.spec_from_file_location('transition',Path(__file__).with_name('compare-apk-source-transition.py'))
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


def apk(commit,payload=b'actual payload',duplicate=False,year=1980):
    buffer=io.BytesIO()
    with zipfile.ZipFile(buffer,'w') as archive:
        for name,data in [('classes.dex',payload),(m.RECORD,m.revision(commit))]:
            info=zipfile.ZipInfo(name,(year,1,1,0,0,0))
            archive.writestr(info,data)
        if duplicate:
            with warnings.catch_warnings():
                warnings.simplefilter('ignore')
                archive.writestr(m.RECORD,m.revision(commit))
    return buffer.getvalue()


class SourceTransition(unittest.TestCase):
    def test_only_expected_source_record_changes(self):
        report=m.compare(apk('a'*40),apk('b'*40),'a'*40,'b'*40)
        self.assertFalse(report['byte_identical'])
        self.assertEqual(report['identical_payload_entries'],1)
        self.assertEqual(report['only_changed_entry'],m.RECORD)

    def test_same_size_changed_application_payload_refused(self):
        with self.assertRaises(ValueError):
            m.compare(apk('a'*40),apk('b'*40,b'edited payload'),'a'*40,'b'*40)

    def test_wrong_source_and_identical_source_claims_refused(self):
        for old,new in [('c'*40,'b'*40),('a'*40,'c'*40),('a'*40,'a'*40)]:
            with self.subTest(old=old,new=new),self.assertRaises(ValueError):
                m.compare(apk('a'*40),apk('b'*40),old,new)

    def test_duplicate_or_timestamp_change_refused(self):
        for new in (apk('b'*40,duplicate=True),apk('b'*40,year=2000)):
            with self.assertRaises(ValueError):m.compare(apk('a'*40),new,'a'*40,'b'*40)


if __name__=='__main__':
    unittest.main()
