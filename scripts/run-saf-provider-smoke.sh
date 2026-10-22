#!/usr/bin/env bash
set -euo pipefail
candidate="${1:?Expected source-bound build artifact directory}"
package=org.cajapersonal.fixmyshot.debug
[[ "${GITHUB_ACTIONS:-}" == true ]] || { echo 'Dedicated disposable CI emulator only' >&2; exit 2; }
[[ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]] || exit 2
api="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
[[ "$api" == 26 || "$api" == 28 ]] || exit 2
source_sha="$(git rev-parse HEAD)"
[[ "$(cat "$candidate/evidence/source-sha.txt")" == "$source_sha" ]] || exit 2
(cd "$candidate" && sha256sum --check evidence/apk-sha256.txt)
mkdir -p evidence
printf '%s\n' "$source_sha" > evidence/source-sha.txt
adb shell getprop ro.build.fingerprint > evidence/emulator-build.txt
token="$(python3 -c 'import secrets; print(secrets.token_hex(16))')"
[[ "$token" =~ ^[0-9a-f]{32}$ ]] || exit 2
fixture="/sdcard/Download/FixMyShotSaf_$token"
# The shell acts as the fixture owner. The application receives no broad storage permission.
adb shell mkdir -p /sdcard/Download
adb shell mkdir "$fixture"
cleanup() {
  # This exact newly created fixture is the only external directory removed by this script.
  adb shell rm -rf "$fixture"
}
trap cleanup EXIT
adb install -r "$candidate/app/build/outputs/apk/debug/app-debug.apk"
adb install -r "$candidate/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
for phase in selectCancelAndPublishThroughActualFolderGrant afterForceStopPersistedGrantIsUsableAndRevocationDeniesReads; do
  if [[ "$phase" == afterForceStop* ]]; then
    adb shell am force-stop "$package"
  fi
  log="evidence/saf-$phase.txt"
  set +e
  timeout 4m adb shell am instrument -w -r \
    -e class "org.cajapersonal.fixmyshot.SafProviderFlowTest#$phase" \
    -e safSmoke true -e safToken "$token" -e sourceSha "$source_sha" \
    "$package.test/androidx.test.runner.AndroidJUnitRunner" > "$log" 2>&1
  result=$?
  set -e
  cat "$log"
  [[ "$result" == 0 ]] || exit 1
  grep -q '^OK (1 test)' "$log"
  if grep -Eq 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed|AssumptionViolated' "$log"; then exit 1; fi
  adb exec-out run-as "$package" cat files/saf-provider-smoke.json > evidence/saf-provider-smoke.json
done
python3 scripts/check-saf-provider.py evidence --source "$source_sha" --api "$api" --token "$token"
