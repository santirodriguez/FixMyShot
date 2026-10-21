#!/usr/bin/env bash
# CI-only fixture setup. Never run against a user device or change the app manifest.
set -euo pipefail
[[ "${GITHUB_ACTIONS:-}" == true ]] || { echo 'Requires an ephemeral Actions emulator' >&2; exit 1; }
[[ "${1:-}" == 26 || "${1:-}" == 36 ]] || { echo 'Expected API 26 or 36' >&2; exit 1; }
property() { adb shell getprop "$1" | tr -d '\r'; }
serial="$(adb get-serialno)"
[[ "$serial" =~ ^emulator-[0-9]+$ ]] || { echo 'Physical/unknown device rejected' >&2; exit 1; }
[[ "$(property ro.kernel.qemu)" == 1 || "$(property ro.boot.qemu)" == 1 ]] || exit 1
[[ "$(property ro.build.version.sdk)" == "$1" ]] || { echo 'Unexpected emulator API' >&2; exit 1; }
mkdir -p evidence
{
  printf 'api=%s\n' "$1"
  printf 'heapgrowthlimit_before=%s\n' "$(property dalvik.vm.heapgrowthlimit)"
  printf 'heapsize_before=%s\n' "$(property dalvik.vm.heapsize)"
} > evidence/emulator-heap-adjustment.txt
# API 26 already honors hw.heapSize. API 36 additionally enforces a fixed growth limit.
if [[ "$1" == 26 ]]; then
  echo 'adjustment=NOT_NEEDED' >> evidence/emulator-heap-adjustment.txt
  exit 0
fi
[[ "$(property ro.debuggable)" == 1 ]] || { echo 'Requires a debuggable emulator' >&2; exit 1; }
# On cold-booted CI emulators adb root can briefly close the transport while restarting adbd.
# Retry only after emulator/API/debuggable guards and still require uid 0 before VM changes.
root_ready=0
for attempt in 1 2 3; do
  adb root >/dev/null 2>&1 || true
  if timeout 20s adb wait-for-device >/dev/null 2>&1 &&
     [[ "$(adb shell id -u 2>/dev/null | tr -d '\r')" == 0 ]]; then
    root_ready=1
    break
  fi
  if (( attempt < 3 )); then sleep 2; fi
done
[[ "$root_ready" == 1 ]] || { echo 'Emulator root unavailable after bounded retries' >&2; exit 1; }
adb shell stop
trap 'adb shell start >/dev/null 2>&1 || true' EXIT
adb shell setprop dalvik.vm.heapsize 512m
adb shell setprop dalvik.vm.heapgrowthlimit 512m
[[ "$(property dalvik.vm.heapsize)" == 512m && "$(property dalvik.vm.heapgrowthlimit)" == 512m ]] || exit 1
adb shell setprop sys.boot_completed 0
adb shell start
trap - EXIT
for ((attempt=0; attempt<90; attempt++)); do
  if [[ "$(property sys.boot_completed)" == 1 ]]; then
    adb shell input keyevent 82
    {
      echo 'adjustment=CI_EMULATOR_ONLY'
      printf 'heapgrowthlimit_after=%s\n' "$(property dalvik.vm.heapgrowthlimit)"
      printf 'heapsize_after=%s\n' "$(property dalvik.vm.heapsize)"
      echo 'acceptance=Actual Runtime.maxMemory remains mandatory in g1-smoke.json'
    } >> evidence/emulator-heap-adjustment.txt
    exit 0
  fi
  sleep 2
done
echo 'Emulator runtime did not restart within the bounded wait' >&2
exit 1
