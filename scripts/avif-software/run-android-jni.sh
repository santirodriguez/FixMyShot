#!/usr/bin/env bash
# Explicitly synthetic, opt-in API 36 JNI test only. Never installs a normal APK.
set -euo pipefail
mkdir -p evidence/avif-jni-runtime
adb install -r candidate/apk-native.apk
adb install -r candidate/apk-native-test.apk
adb shell am instrument -w \
  -e class org.cajapersonal.fixmyshot.avifprobe.AvifSoftwareProbeTest \
  org.cajapersonal.fixmyshot.avifprobe.test/androidx.test.runner.AndroidJUnitRunner \
  | tee evidence/avif-jni-runtime/instrumentation.txt
grep -q 'OK (1 test)' evidence/avif-jni-runtime/instrumentation.txt
pkg=org.cajapersonal.fixmyshot.avifprobe
adb exec-out run-as "$pkg" cat files/avif_probe_metrics.json \
  > evidence/avif-jni-runtime/instrumented-metrics.json
adb exec-out run-as "$pkg" cat files/avif_probe_65x67.avif \
  > evidence/avif-jni-runtime/fixture-65x67.avif
adb exec-out run-as "$pkg" cat files/avif_probe_1344x2992.avif \
  > evidence/avif-jni-runtime/fixture-1344x2992.avif
adb shell getconf PAGESIZE > evidence/avif-jni-runtime/emulator-page-size.txt
python3 scripts/avif-software/apk_delta.py metrics \
  evidence/avif-jni-runtime/instrumented-metrics.json \
  > evidence/avif-jni-runtime/summary.json
python3 scripts/avif-software/check_fixture.py \
  evidence/avif-jni-runtime/fixture-65x67.avif \
  > evidence/avif-jni-runtime/quality-65x67.json
ffprobe -v error -select_streams v:0 \
  -show_entries stream=width,height,pix_fmt,color_range \
  -of json evidence/avif-jni-runtime/fixture-1344x2992.avif \
  > evidence/avif-jni-runtime/large-geometry.json
python3 - <<'PY'
import json
from pathlib import Path
data = json.loads(Path('evidence/avif-jni-runtime/large-geometry.json').read_text())
stream = data['streams'][0]
assert (stream['width'], stream['height']) == (1344, 2992), stream
assert stream['pix_fmt'] == 'yuv444p', stream
assert stream.get('color_range') == 'pc', stream
PY
