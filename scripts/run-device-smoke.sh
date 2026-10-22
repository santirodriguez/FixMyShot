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
adb shell run-as "$package" rm -f files/g1-smoke.json files/g1-fixture-0.png files/g1-fixture-0.webp files/g1-native-probe.json files/g1-native-64x64.heic files/g1-native-64x64.avif files/g1-native-65x67.heic files/g1-native-65x67.avif files/g1-grid-diagnostic.json files/g1-heic-pixel-layout.json files/g1-grid-heic-64x64-off.heic files/g1-grid-heic-65x67-off.heic files/g1-grid-heic-66x68-off.heic files/g1-grid-heic-66x68-on.heic files/g1-grid-avif-64x64-off.avif files/g1-grid-avif-65x67-off.avif files/g1-grid-avif-66x68-off.avif files/g1-grid-avif-66x68-on.avif files/g1-grid-heic-96x68-off.heic files/g1-grid-avif-96x68-off.avif files/g1-grid-heic-128x96-off.heic files/g1-grid-avif-128x96-off.avif
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
for size in 64x64 65x67; do
  for extension in heic avif; do
    native_output="evidence/g1-native-$size.$extension"
    if adb exec-out run-as "$package" cat "files/g1-native-$size.$extension" > "$native_output"; then
      # adb may succeed while returning a device-side missing-file error as stdout.
      if [[ "$(dd if="$native_output" bs=1 skip=4 count=4 2>/dev/null)" != "ftyp" ]]; then
        rm -f "$native_output"
      fi
    else
      rm -f "$native_output"
    fi
  done
done
# Experimental grid/padding comparison is separate from the existing native capability probe.
# An output padded from 65x67 to 66x68 is NEVER advertised as a 65x67 export.
timeout 4m adb shell am instrument -w -r \
  -e class org.cajapersonal.fixmyshot.NativeGridDiagnosticTest \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" > evidence/grid-instrumentation.txt 2>&1
grid_result=$?
cat evidence/grid-instrumentation.txt
adb exec-out run-as "$package" cat files/g1-grid-diagnostic.json > evidence/g1-grid-diagnostic.json
grid_report_result=$?
for format in heic avif; do
  for variant in 64x64-off 65x67-off 66x68-off 66x68-on 96x68-off 128x96-off; do
    grid_file="evidence/g1-grid-$format-$variant.$format"
    if adb exec-out run-as "$package" cat "files/g1-grid-$format-$variant.$format" > "$grid_file"; then
      if [[ "$(dd if="$grid_file" bs=1 skip=4 count=4 2>/dev/null)" != "ftyp" ]]; then
        rm -f "$grid_file"
      fi
    else
      rm -f "$grid_file"
    fi
  done
done
# Compare retained synthetic HEIC files with Android decoder paths.
timeout 2m adb shell am instrument -w -r \
  -e class org.cajapersonal.fixmyshot.NativeHeicPixelLayoutTest \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" > evidence/heic-layout-instrumentation.txt 2>&1
layout_result=$?
cat evidence/heic-layout-instrumentation.txt
adb exec-out run-as "$package" cat files/g1-heic-pixel-layout.json > evidence/g1-heic-pixel-layout.json
layout_report_result=$?
# Verify the new plan-driven renderer with synthetic bitmaps, separately from codec evidence.
timeout 2m adb shell am instrument -w -r \
  -e class org.cajapersonal.fixmyshot.ReviewRenderTest \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" > evidence/review-render-instrumentation.txt 2>&1
review_result=$?
cat evidence/review-render-instrumentation.txt
# Cache ownership/eviction is checked separately using only synthetic bitmaps.
timeout 2m adb shell am instrument -w -r \
  -e class org.cajapersonal.fixmyshot.ReviewThumbnailCacheTest \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" > evidence/thumbnail-instrumentation.txt 2>&1
thumbnail_result=$?
cat evidence/thumbnail-instrumentation.txt
# The private executor uses synthetic staged bytes; it never publishes outputs.
timeout 4m adb shell am instrument -w -r \
  -e class org.cajapersonal.fixmyshot.PrivateOutputExecutorTest \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" > evidence/private-output-instrumentation.txt 2>&1
private_result=$?
cat evidence/private-output-instrumentation.txt
# The nine scoped save tests use a fake provider plus one synthetic API29+ MediaStore item.
# API26 exercises only the permission-free fake transaction and skips the API29 provider branch.
timeout 4m adb shell am instrument -w -r \
  -e class org.cajapersonal.fixmyshot.TransactionalOutputSaverTest \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" > evidence/transaction-instrumentation.txt 2>&1
transaction_result=$?
cat evidence/transaction-instrumentation.txt
# From app-private source through actual codec, validated provider and UI state.
timeout 4m adb shell am instrument -w -r \
  -e class org.cajapersonal.fixmyshot.ReviewExportFlowTest \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" > evidence/export-flow-instrumentation.txt 2>&1
export_flow_result=$?
cat evidence/export-flow-instrumentation.txt
# A separate fault-injected batch suite checks sequential output state and restart barriers.
timeout 4m adb shell am instrument -w -r \
  -e class org.cajapersonal.fixmyshot.BatchOutputCoordinatorTest \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" > evidence/batch-instrumentation.txt 2>&1
batch_result=$?
cat evidence/batch-instrumentation.txt
# Prior-job discovery and item-journal reconciliation use synthetic app-owned files only.
timeout 3m adb shell am instrument -w -r \
  -e class org.cajapersonal.fixmyshot.BatchRecoveryScannerTest,org.cajapersonal.fixmyshot.BatchRecoveryReconciliationTest,org.cajapersonal.fixmyshot.ReviewRecoveryUiTest \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" > evidence/batch-recovery-instrumentation.txt 2>&1
batch_recovery_result=$?
cat evidence/batch-recovery-instrumentation.txt
set -e
grep -q 'OK (4 tests)' evidence/review-render-instrumentation.txt
grep -q 'OK (3 tests)' evidence/thumbnail-instrumentation.txt
grep -q 'OK (6 tests)' evidence/private-output-instrumentation.txt
grep -q 'OK (9 tests)' evidence/transaction-instrumentation.txt
grep -q 'OK (3 tests)' evidence/export-flow-instrumentation.txt
grep -q 'OK (5 tests)' evidence/batch-instrumentation.txt
grep -q 'OK (4 tests)' evidence/batch-recovery-instrumentation.txt
# Successful adb exits alone do not certify files, instrumentation or coverage.
python3 scripts/check-device-report.py evidence
python3 scripts/check-native-probe.py evidence
python3 scripts/check-native-grid.py evidence
python3 scripts/check-heic-layout.py evidence
[[ "$result" -eq 0 && "$native_result" -eq 0 && "$native_report_result" -eq 0 &&
   "$grid_result" -eq 0 && "$grid_report_result" -eq 0 &&
   "$layout_result" -eq 0 && "$layout_report_result" -eq 0 &&
   "$review_result" -eq 0 && "$thumbnail_result" -eq 0 &&
   "$private_result" -eq 0 && "$transaction_result" -eq 0 && "$batch_result" -eq 0 &&
   "$export_flow_result" -eq 0 && "$batch_recovery_result" -eq 0 ]]
