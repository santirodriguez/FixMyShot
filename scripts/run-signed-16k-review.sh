#!/usr/bin/env bash
# CI-only exact signed APK probe, on a disposable Android 15 16 KiB emulator.
# Do not rebuild, re-sign, upload private photos, or claim native export was tested.
set -euo pipefail
source_dir="$1"
out=evidence/signed-16k
mkdir -p "$out"
app="$source_dir/FixMyShot-1.0.0-universal.apk"
expected=92efebea8ebfce6a88865a8b4842f61e4362d8951fb7949ea626c0a0245ea428
[[ "$GITHUB_ACTIONS" == true && -f "$app" ]] || { echo 'CI-only original signed APK required' >&2; exit 2; }
printf '%s  %s\n' "$expected" "$app" | sha256sum -c - | tee "$out/apk-hash.txt"
[[ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" == 1 ]] || { echo 'Not an emulator' >&2; exit 2; }
adb shell getprop ro.build.fingerprint > "$out/build.txt"
adb shell getprop ro.build.version.sdk | tr -d '\r' > "$out/api.txt"
adb shell getprop ro.product.cpu.abi | tr -d '\r' > "$out/abi.txt"
adb shell getconf PAGE_SIZE | tr -d '\r' > "$out/page-size.txt"
[[ "$(cat "$out/api.txt")" == 35 && "$(cat "$out/abi.txt")" == x86_64 && "$(cat "$out/page-size.txt")" == 16384 ]] || { echo 'Wrong Android 16 KiB target' >&2; exit 2; }
adb install -r "$app" | tee "$out/install.txt"
grep -q Success "$out/install.txt" || { echo 'Install did not succeed' >&2; exit 2; }
adb shell pm path org.cajapersonal.fixmyshot > "$out/package-path.txt"
grep -q 'package:' "$out/package-path.txt" || { echo 'Package not found' >&2; exit 2; }
adb shell dumpsys package org.cajapersonal.fixmyshot > "$out/package-dump.txt"
grep -q 'versionCode=1 ' "$out/package-dump.txt" || { echo 'Unexpected versionCode' >&2; exit 2; }
grep -q 'versionName=1.0.0' "$out/package-dump.txt" || { echo 'Unexpected versionName' >&2; exit 2; }
adb shell am start -W -n org.cajapersonal.fixmyshot/.MainActivity | tee "$out/launch.txt"
grep -q 'Status: ok' "$out/launch.txt" || { echo 'Launch failed' >&2; exit 2; }
sleep 5
adb shell pidof org.cajapersonal.fixmyshot | tr -d '\r' > "$out/pid.txt"
[[ -s "$out/pid.txt" ]] || { echo 'App process exited after initial launch' >&2; exit 2; }
adb shell dumpsys activity activities > "$out/activities.txt"
adb shell logcat -d -v threadtime -s AndroidRuntime:E libc:F > "$out/runtime-errors.txt" || true
adb shell am force-stop org.cajapersonal.fixmyshot
adb shell am start -W -n org.cajapersonal.fixmyshot/.MainActivity | tee "$out/relaunch.txt"
grep -q 'Status: ok' "$out/relaunch.txt" || { echo 'Relaunch failed' >&2; exit 2; }
sleep 3
adb shell pidof org.cajapersonal.fixmyshot | tr -d '\r' > "$out/relaunch-pid.txt"
[[ -s "$out/relaunch-pid.txt" ]] || { echo 'App process exited after relaunch' >&2; exit 2; }
echo 'PASS_SIGNED_APK_INSTALL_LAUNCH_RELAUNCH_EMULATED_16K__NATIVE_EXPORT_NOT_EXERCISED' | tee "$out/verdict.txt"
