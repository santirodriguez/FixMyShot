#!/usr/bin/env python3
"""Class trace boundary, coverage and origin refusal checks; no JVM/network needed."""
import importlib.util
from pathlib import Path
import tempfile
import unittest

spec=importlib.util.spec_from_file_location('audit',Path(__file__).with_name('audit-build-classloads.py'))
m=importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)


def event(name,origin='shared objects file'):
    return '[0.001s][info][class,load] '+name+' source: '+origin+'\n'


class ClassLoads(unittest.TestCase):
    def setUp(self):
        temp=tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root=Path(temp.name)
        self.file=self.root/'classes-123.log'
        self.good=event('java.lang.Object')+''.join(event(prefix+'Fixture') for prefix in m.ROLES.values())
        self.file.write_text(self.good)

    def audit(self):return m.audit(self.root,'a'*40,'b'*40,[])

    def test_positive_class_origin_mapping_and_no_global_clearance(self):
        self.file.write_text(self.good+event('io.netty.handler.ssl.SslHandler','file:/not-approved/netty.jar'))
        report=self.audit()
        self.assertEqual(report['families']['netty']['distinct_class_origin_pairs'],1)
        self.assertIsNone(report['origins'][0]['local_jar_sha256'])
        self.assertFalse(report['security_gate_automatically_closed'])
        self.assertFalse(report['class_presence_means_method_execution'])

    def test_exact_package_boundaries(self):
        self.file.write_text(self.good+event('io.nettyish.Fake')+event('not.org.bouncycastle.Fake'))
        self.assertTrue(all(not x['classes'] for x in self.audit()['families'].values()))

    def test_missing_role_or_startup_marker_refused(self):
        for old in [event('java.lang.Object'),event(m.ROLES['lint']+'Fixture')]:
            self.file.write_text(self.good.replace(old,''))
            with self.assertRaises(ValueError):self.audit()

    def test_rotated_partial_and_malformed_trace_refused(self):
        (self.root/'classes-123.log.0').write_text(self.good)
        with self.assertRaises(ValueError):self.audit()
        (self.root/'classes-123.log.0').unlink()
        for data in [self.good[:-1],self.good+'unparsed event\n','']:
            self.file.write_text(data)
            with self.assertRaises(ValueError):self.audit()

    def test_empty_and_symlinked_capture_refused(self):
        self.file.unlink()
        with self.assertRaises(ValueError):self.audit()
        self.file.symlink_to(self.root/'missing.log')
        with self.assertRaises(ValueError):self.audit()


if __name__=='__main__':unittest.main()
