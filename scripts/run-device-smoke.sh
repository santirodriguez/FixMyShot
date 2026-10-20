#!/usr/bin/env bash
set -euo pipefail
mkdir -p evidence
git rev-parse HEAD > evidence/source-sha.txt
adb shell getprop ro.build.fingerprint > evidence/emulator-build.txt
adb shell getconf PAGE_SIZE > evidence/page-size.txt
set +e
./gradlew --no-daemon :app:connectedDebugAndroidTest
result=$?
adb exec-out run-as org.cajapersonal.fixmyshot.debug cat files/g1-smoke.json > evidence/g1-smoke.json
report_result=$?
set -e
if [[ "$report_result" -ne 0 ]]; then
  echo 'G1 device report unavailable' >&2
  rm -f evidence/g1-smoke.json
fi
exit "$result"
