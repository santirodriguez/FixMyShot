#!/usr/bin/env bash
# Explicit, one-off source-built Android ABI feasibility experiment; never an app encoder.
# Run only in an authorized disposable Android build environment, not on a user's device.
set -euo pipefail

readonly JXL_REF='a7a9c787341cf703dede03c2009fa460cae5e5df'
readonly JXL_TAG='v0.12.0'
readonly NDK_VERSION='28.2.13676358'
readonly ABIS=(arm64-v8a armeabi-v7a x86_64)

if [[ $# -ne 1 ]]; then
  echo 'Usage: bash scripts/probe-jxl-reference.sh <absolute-disposable-work-directory>' >&2
  exit 2
fi
root="$1"
if [[ "$root" != /* || "$root" == / || "$root" == /mnt/data || -e "$root/source" ]]; then
  echo 'Requires a new dedicated absolute work directory; no existing source may be replaced' >&2
  exit 2
fi
ndk="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-${ANDROID_HOME:-}/ndk/$NDK_VERSION}}"
if [[ ! -f "$ndk/build/cmake/android.toolchain.cmake" ]]; then
  echo 'NOT_RUN: pinned Android NDK r28 toolchain unavailable; do not claim Android build' >&2
  exit 2
fi
for cmd in git cmake ninja python3 timeout; do
  command -v "$cmd" >/dev/null || { echo "Missing required command: $cmd" >&2; exit 2; }
done
mkdir -p "$root"
mkdir -p "$root/evidence"
printf 'reference_commit=%s\ntag=%s\nandroid_ndk=%s\n' "$JXL_REF" "$JXL_TAG" "$NDK_VERSION" > "$root/evidence/parameters.txt"
# Pinned tag is verified against its full immutable commit; recursive submodules use upstream gitlinks.
# Time bounds prevent a hanging fetch from burning a CI run.
timeout 300s git clone --depth 1 --branch "$JXL_TAG" --recurse-submodules --shallow-submodules \
  https://github.com/libjxl/libjxl.git "$root/source" > "$root/evidence/clone.log" 2>&1
actual="$(git -C "$root/source" rev-parse HEAD)"
[[ "$actual" == "$JXL_REF" ]] || { echo "Pinned JXL tag mismatch: $actual" >&2; exit 1; }
git -C "$root/source" submodule status --recursive > "$root/evidence/submodules.txt"
# All ABI builds are scoped to API26 without touching the application's current codec adapter.
for abi in "${ABIS[@]}"; do
  target="$root/build-$abi"
  printf '%s\n' "$abi" > "$root/evidence/abi-$abi.txt"
  timeout 360s cmake -S "$root/source" -B "$target" -G Ninja \
    -DCMAKE_TOOLCHAIN_FILE="$ndk/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$abi" -DANDROID_PLATFORM=android-26 \
    -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=ON \
    -DBUILD_TESTING=OFF -DJPEGXL_ENABLE_TOOLS=OFF \
    -DJPEGXL_ENABLE_EXAMPLES=OFF -DJPEGXL_ENABLE_BENCHMARK=OFF \
    -DJPEGXL_ENABLE_DEVTOOLS=OFF -DJPEGXL_ENABLE_JNI=OFF \
    -DJPEGXL_ENABLE_DOXYGEN=OFF -DJPEGXL_ENABLE_MANPAGES=OFF \
    -DJPEGXL_ENABLE_FUZZERS=OFF -DJPEGXL_ENABLE_VIEWERS=OFF \
    -DJPEGXL_ENABLE_PLUGINS=OFF -DJPEGXL_ENABLE_TCMALLOC=OFF \
    -DJPEGXL_ENABLE_SJPEG=OFF -DJPEGXL_ENABLE_OPENEXR=OFF \
    -DJPEGXL_ENABLE_LTO=OFF > "$root/evidence/configure-$abi.log" 2>&1
  /usr/bin/time -f 'build_wall_seconds=%e\nmaxrss_kib=%M' -o "$root/evidence/build-metrics-$abi.txt" \
    timeout 720s cmake --build "$target" --target jxl jxl_dec jxl_threads --parallel 2 \
    > "$root/evidence/compile-$abi.log" 2>&1
  # Record every .so in the source-built tree, including needed transitives.
  find "$target" -type f -name '*.so*' -printf '%p\t%s bytes\n' | sort \
    > "$root/evidence/libs-$abi.txt"
  if [[ ! -s "$root/evidence/libs-$abi.txt" ]]; then
    echo "FAIL: no Android native libraries built for $abi" >&2
    exit 1
  fi
done
python3 - "$root" <<'PY'
from pathlib import Path
import json, sys
p=Path(sys.argv[1]); measurements={}
for abi in ('arm64-v8a','armeabi-v7a','x86_64'):
    candidates=sorted(x for x in (p/f'build-{abi}').rglob('*.so*') if x.is_file() and not x.is_symlink())
    measurements[abi]={'shared_library_files':len(candidates), 'raw_bytes':sum(x.stat().st_size for x in candidates)}
report={'status':'SOURCE_BUILD_ATTEMPT_COMPLETE','libjxl_git_commit':'a7a9c787341cf703dede03c2009fa460cae5e5df',
  'scope':'Unstripped Android shared-library build and raw sizes only; not a linked JNI APK, no device/16KiB/color/cancellation acceptance',
  'abis':measurements}
(p/'evidence/jxl-probe-summary.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
PY
