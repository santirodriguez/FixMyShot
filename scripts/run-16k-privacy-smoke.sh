#!/usr/bin/env bash
# Opt-in CI-only, disposable emulator; never reaches user or release media.
set -euo pipefail
candidate="${1:?Expected exact same-run debug build artifact directory}"
package=org.cajapersonal.fixmyshot.visualspacing
out=evidence/16k-privacy
[[ "${GITHUB_ACTIONS:-}" == true ]] || { echo 'CI-only validation' >&2; exit 2; }
[[ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]] || {
  echo 'Refusing non-emulator target' >&2; exit 2;
}
[[ "$(cat "$candidate/evidence/source-sha.txt")" == "$(git rev-parse HEAD)" ]] || {
  echo 'Candidate source commit mismatch' >&2; exit 2;
}
(cd "$candidate" && sha256sum --check evidence/apk-sha256.txt)
mkdir -p "$out"
adb shell getprop ro.build.fingerprint > "$out/build-fingerprint.txt"
adb shell getprop ro.build.version.sdk | tr -d '\r' > "$out/android-api.txt"
adb shell getprop ro.product.cpu.abi | tr -d '\r' > "$out/android-abi.txt"
adb shell getconf PAGE_SIZE | tr -d '\r' > "$out/page-size-shell.txt"
[[ "$(cat "$out/android-api.txt")" == 35 && "$(cat "$out/android-abi.txt")" == x86_64 &&
   "$(cat "$out/page-size-shell.txt")" == 16384 ]] || {
  echo 'No actual Android 35/x86_64/16384-byte runtime' >&2; exit 2;
}
# Only source-bound debug and its instrumentation companion; no signed APK or Actions read permission.
adb install -r "$candidate/app/build/outputs/apk/debug/app-debug.apk"
adb install -r "$candidate/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
classes=org.cajapersonal.fixmyshot.CodecSmokeTest,org.cajapersonal.fixmyshot.MetadataPolicyTest,org.cajapersonal.fixmyshot.SixteenKPrivacyTest
set +e
timeout 22m adb shell am instrument -w -r -e class "$classes" \
  "$package.test/androidx.test.runner.AndroidJUnitRunner" > "$out/instrumentation.txt" 2>&1
result=$?
set -e
cat "$out/instrumentation.txt"
[[ "$result" -eq 0 ]] || { echo 'Android instrumentation failed' >&2; exit 1; }
for extension in png webp; do
  adb exec-out run-as "$package" cat "files/g1-fixture-0.$extension" > "$out/g1-fixture-0.$extension"
  for mode in default preserve; do
    adb exec-out run-as "$package" cat "files/ci-16k-privacy/privacy-$mode.$extension" \
      > "$out/privacy-$mode.$extension"
  done
done
adb exec-out run-as "$package" cat files/g1-smoke.json > "$out/g1-smoke.json"
adb exec-out run-as "$package" cat files/ci-16k-privacy/native-webp-16k.webp > "$out/native-webp-16k.webp"
python3 scripts/check-16k-privacy-evidence.py "$out"
