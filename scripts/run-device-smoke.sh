#!/usr/bin/env bash
set -euo pipefail
candidate="${1:-.}"
package=org.cajapersonal.fixmyshot.debug
mkdir -p evidence
source_sha="$(git rev-parse HEAD)"
printf '%s\n' "$source_sha" > evidence/source-sha.txt
if [[ "$candidate" != . ]]; then
  [[ "$(cat "$candidate/evidence/source-sha.txt")" == "$source_sha" ]] || {
    echo 'Candidate source does not match checkout' >&2; exit 1;
  }
  (cd "$candidate" && sha256sum --check evidence/apk-sha256.txt)
fi
adb shell getprop ro.build.fingerprint > evidence/emulator-build.txt
adb shell getprop dalvik.vm.heapgrowthlimit > evidence/heap-growth-limit.txt
adb shell getprop dalvik.vm.heapsize > evidence/heap-size.txt
# API 26 has no getconf binary. The authoritative page size is measured in-app.
if ! adb shell getconf PAGE_SIZE > evidence/page-size-shell.txt 2>/dev/null; then
  printf '%s\n' 'UNAVAILABLE: use page_size_bytes in g1-smoke.json' > evidence/page-size-shell.txt
fi
adb install -r "$candidate/app/build/outputs/apk/debug/app-debug.apk"
adb install -r "$candidate/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
adb shell run-as "$package" rm -f files/g1-smoke.json files/g1-fixture-0.png files/g1-fixture-0.webp files/g1-native-probe.json files/g1-native-fixture.heic files/g1-native-fixture.avif
set +e
timeout 12m adb shell am instrument -w -r \
  -e class org.cajapersonal.fixmyshot.CodecSmokeTest \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" > evidence/instrumentation.txt 2>&1
result=$?
cat evidence/instrumentation.txt
adb exec-out run-as "$package" cat files/g1-smoke.json > evidence/g1-smoke.json
for extension in png webp; do
  adb exec-out run-as "$package" cat "files/g1-fixture-0.$extension" > "evidence/g1-fixture-0.$extension"
done
# Native writers run separately so the established four-test lossless matrix remains comparable.
timeout 3m adb shell am instrument -w -r \
  -e class org.cajapersonal.fixmyshot.NativeCodecProbeTest \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" > evidence/native-instrumentation.txt 2>&1
native_result=$?
cat evidence/native-instrumentation.txt
adb exec-out run-as "$package" cat files/g1-native-probe.json > evidence/g1-native-probe.json
native_report_result=$?
for extension in heic avif; do
  native_output="evidence/g1-native-fixture.$extension"
  if adb exec-out run-as "$package" cat "files/g1-native-fixture.$extension" > "$native_output"; then
    # Some adb/run-as combinations return 0 while stdout contains a device-side cat error.
    # Such text is not a file. Positive probe reports still require a real BMFF in the host validator.
    if [[ "$(dd if="$native_output" bs=1 skip=4 count=4 2>/dev/null)" != "ftyp" ]]; then
      rm -f "$native_output"
    fi
  else
    rm -f "$native_output"
  fi
done
set -e
# Successful adb exits alone do not certify images, sample coverage or native capabilities.
python3 scripts/check-device-report.py evidence
python3 scripts/check-native-probe.py evidence
[[ "$result" -eq 0 && "$native_result" -eq 0 && "$native_report_result" -eq 0 ]]
