#!/usr/bin/env python3
"""Verify the CI heap helper never mutates a non-test device or hides a setup failure."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

HELPER = Path(__file__).with_name('configure-emulator-heap.sh').resolve()
MOCK = '''#!/usr/bin/env bash
set -eu
if [[ "${1:-}" == -s ]]; then
  [[ "${2:-}" == emulator-5554 ]] || exit 99
  shift 2
fi
printf '%s\\n' "$*" >> adb-calls.txt
case "$*" in
 'get-serialno') echo "${SERIAL:-emulator-5554}" ;;
 'shell getprop ro.kernel.qemu'|'shell getprop ro.boot.qemu') echo "${QEMU:-1}" ;;
 'shell getprop ro.build.version.sdk') echo "${API:-36}" ;;
 'shell getprop ro.debuggable') echo "${DEBUGGABLE:-1}" ;;
 'shell getprop dalvik.vm.heapgrowthlimit')
   if [[ -f growth ]]; then cat growth; else echo 192m; fi ;;
 'shell getprop dalvik.vm.heapsize')
   if [[ -f heap ]]; then cat heap; else echo 576m; fi ;;
 'shell getprop sys.boot_completed') echo 1 ;;
 'root')
   if [[ "${ROOT_NEVER:-0}" == 1 ]]; then exit 1; fi
   if [[ "${ROOT_ONCE:-0}" == 1 && ! -f root-failed ]]; then touch root-failed; exit 1; fi
   touch root-ready ;;
 'shell id -u')
   if [[ "${ROOT_LOST_AFTER_STOP:-0}" == 1 && -f stop-failed ]]; then echo 2000; exit 0; fi
   if [[ "${ROOT_NEVER:-0}" == 1 || ( "${ROOT_ONCE:-0}" == 1 && ! -f root-ready ) ]]; then echo 2000; else echo 0; fi ;;
 'shell stop')
   if [[ "${STOP_NEVER:-0}" == 1 ]]; then exit 255; fi
   if [[ "${STOP_ONCE:-0}" == 1 && ! -f stop-failed ]]; then touch stop-failed; exit 255; fi ;;
 'shell setprop dalvik.vm.heapgrowthlimit 512m')
   if [[ "${REFUSE_PROPERTY:-0}" != 1 ]]; then echo 512m > growth; fi ;;
 'shell setprop dalvik.vm.heapsize 512m') echo 512m > heap ;;
esac
'''


class EmulatorHeapTests(unittest.TestCase):
    def run_helper(self, expected=36, **environment):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        root = Path(temp.name)
        mock = root / 'adb'
        mock.write_text(MOCK)
        mock.chmod(0o755)
        env = dict(os.environ, GITHUB_ACTIONS='true', PATH=str(root) + os.pathsep + os.environ['PATH'])
        env.update(environment)
        result = subprocess.run(['bash', str(HELPER), str(expected)], cwd=root, env=env,
                                capture_output=True, text=True, timeout=15)
        log = (root / 'adb-calls.txt').read_text() if (root / 'adb-calls.txt').exists() else ''
        return result, log, root

    def test_outside_ci_does_not_touch_adb(self):
        result, log, _ = self.run_helper(GITHUB_ACTIONS='false')
        self.assertNotEqual(0, result.returncode)
        self.assertEqual('', log)

    def test_physical_device_rejected(self):
        result, log, _ = self.run_helper(SERIAL='physical-device')
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn('root', log)
        self.assertNotIn('setprop', log)

    def test_non_emulator_property_rejected(self):
        result, log, _ = self.run_helper(QEMU='0')
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn('root', log)

    def test_wrong_api_rejected(self):
        result, log, _ = self.run_helper(API='35')
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn('root', log)

    def test_non_debuggable_emulator_rejected(self):
        result, log, _ = self.run_helper(DEBUGGABLE='0')
        self.assertNotEqual(0, result.returncode)
        self.assertNotIn('root', log)

    def test_api26_is_not_mutated(self):
        result, log, root = self.run_helper(expected=26, API='26')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertNotIn('root', log)
        self.assertNotIn('setprop', log)
        self.assertIn('NOT_NEEDED', (root / 'evidence/emulator-heap-adjustment.txt').read_text())

    def test_api36_records_scoped_adjustment(self):
        result, log, root = self.run_helper()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn('shell stop', log)
        self.assertIn('shell start', log)
        self.assertIn('heapgrowthlimit_after=512m', (root / 'evidence/emulator-heap-adjustment.txt').read_text())

    def test_transient_adb_root_transport_retries_only_on_guarded_emulator(self):
        result, log, root = self.run_helper(ROOT_ONCE='1')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(2, len([call for call in log.splitlines() if call == 'root']))
        self.assertIn('adjustment=CI_EMULATOR_ONLY', (root / 'evidence/emulator-heap-adjustment.txt').read_text())

    def test_unavailable_root_remains_a_failure_without_setprop(self):
        result, log, _ = self.run_helper(ROOT_NEVER='1')
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(3, len([call for call in log.splitlines() if call == 'root']))
        self.assertNotIn('setprop', log)
        self.assertNotIn('shell stop', log)

    def test_stop_disconnect_recovers_same_emulator_once(self):
        result, log, root = self.run_helper(STOP_ONCE='1')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(2, log.splitlines().count('shell stop'))
        self.assertIn('stage=stop-framework:reconnect', (root / 'evidence/emulator-heap-adjustment.txt').read_text())
        self.assertIn('heapgrowthlimit_after=512m', (root / 'evidence/emulator-heap-adjustment.txt').read_text())

    def test_permanent_stop_failure_restores_and_does_not_set_properties(self):
        result, log, root = self.run_helper(STOP_NEVER='1')
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(2, log.splitlines().count('shell stop'))
        self.assertIn('shell start', log)
        self.assertNotIn('setprop', log)
        self.assertNotIn('adjustment=CI_EMULATOR_ONLY', (root / 'evidence/emulator-heap-adjustment.txt').read_text())

    def test_reconnect_without_root_cannot_mutate_heap(self):
        result, log, root = self.run_helper(STOP_ONCE='1', ROOT_LOST_AFTER_STOP='1')
        self.assertNotEqual(0, result.returncode)
        self.assertEqual(1, log.splitlines().count('shell stop'))
        self.assertNotIn('setprop', log)
        self.assertNotIn('adjustment=CI_EMULATOR_ONLY', (root / 'evidence/emulator-heap-adjustment.txt').read_text())

    def test_failed_property_change_is_not_success(self):
        result, log, _ = self.run_helper(REFUSE_PROPERTY='1')
        self.assertNotEqual(0, result.returncode)
        self.assertIn('shell start', log)


if __name__ == '__main__':
    unittest.main()
