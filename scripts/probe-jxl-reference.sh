#!/usr/bin/env bash
# Bounded, explicit source-build feasibility test only; never ship its output or enable JXL.
set -euo pipefail

readonly JXL_REF='a7a9c787341cf703dede03c2009fa460cae5e5df'
readonly JXL_TAG='v0.12.0'
readonly NDK_VERSION='28.2.13676358'
readonly ABI_REQUEST="${JXL_PROBE_ABI:-all}"
case "$ABI_REQUEST" in
  all) readonly ABIS=(arm64-v8a armeabi-v7a x86_64) ;;
  arm64-v8a|armeabi-v7a|x86_64) readonly ABIS=("$ABI_REQUEST") ;;
  *) echo 'Invalid JXL_PROBE_ABI; expected all or a listed Android ABI' >&2; exit 2 ;;
esac
readonly SUBMODULES=(third_party/brotli third_party/highway third_party/skcms)

if [[ $# -ne 1 ]]; then
  echo 'Usage: bash scripts/probe-jxl-reference.sh <new-absolute-disposable-work-directory>' >&2
  exit 2
fi
root="$1"
if [[ "$root" != /* || "$root" == / || "$root" == /mnt/data || -e "$root" ]]; then
  echo 'Use a new absolute disposable directory; existing data must not be overwritten' >&2
  exit 2
fi
ndk="${ANDROID_NDK_HOME:-${ANDROID_NDK_ROOT:-${ANDROID_HOME:-}/ndk/$NDK_VERSION}}"
if [[ ! -f "$ndk/build/cmake/android.toolchain.cmake" ]] ||
   ! grep -Eq "^Pkg.Revision[[:space:]]*=[[:space:]]*$NDK_VERSION([[:space:]]*)$" "$ndk/source.properties" 2>/dev/null; then
  echo 'NOT_RUN: exact NDK r28.2.13676358 unavailable; no Android build claimed' >&2
  exit 2
fi
for cmd in git cmake ninja python3 timeout; do
  command -v "$cmd" >/dev/null || { echo "Missing required command: $cmd" >&2; exit 2; }
done
mkdir -p "$root/evidence"
report="$root/evidence"
printf 'tag=%s\npinned_commit=%s\nndk_revision=%s\nsource=github.com/libjxl/libjxl\n' \
  "$JXL_TAG" "$JXL_REF" "$NDK_VERSION" > "$report/parameters.txt"

# Download ONLY required upstream gitlink-pinned source modules, no testdata or tools.
timeout 240s git clone --quiet --depth 1 --branch "$JXL_TAG" \
  https://github.com/libjxl/libjxl.git "$root/source" > "$report/clone.log" 2>&1
actual="$(git -C "$root/source" rev-parse HEAD)"
[[ "$actual" == "$JXL_REF" ]] || { echo "FAIL: pinned tag resolved to $actual" >&2; exit 1; }
timeout 240s git -C "$root/source" submodule update --init --depth 1 --recommend-shallow \
  -- "${SUBMODULES[@]}" >> "$report/clone.log" 2>&1
for module in "${SUBMODULES[@]}"; do
  gitlink="$(git -C "$root/source" ls-tree HEAD "$module" | awk '{print $3}')"
  resolved="$(git -C "$root/source/$module" rev-parse HEAD)"
  [[ -n "$gitlink" && "$gitlink" == "$resolved" ]] || {
    echo "FAIL: submodule $module does not match source gitlink" >&2; exit 1;
  }
  printf '%s %s\n' "$module" "$resolved" >> "$report/submodules.txt"
done

for abi in "${ABIS[@]}"; do
  folder="$root/build-$abi"
  printf 'ABI=%s\n' "$abi" > "$report/abi-$abi.txt"
  timeout 180s cmake -S "$root/source" -B "$folder" -G Ninja \
    -DCMAKE_TOOLCHAIN_FILE="$ndk/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$abi" -DANDROID_PLATFORM=android-26 \
    -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON \
    -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=ON \
    -DBUILD_TESTING=OFF -DJPEGXL_ENABLE_TOOLS=OFF \
    -DJPEGXL_ENABLE_EXAMPLES=OFF -DJPEGXL_ENABLE_BENCHMARK=OFF \
    -DJPEGXL_ENABLE_DEVTOOLS=OFF -DJPEGXL_ENABLE_JNI=OFF \
    -DJPEGXL_ENABLE_DOXYGEN=OFF -DJPEGXL_ENABLE_MANPAGES=OFF \
    -DJPEGXL_ENABLE_FUZZERS=OFF -DJPEGXL_ENABLE_VIEWERS=OFF \
    -DJPEGXL_ENABLE_PLUGINS=OFF -DJPEGXL_ENABLE_TCMALLOC=OFF \
    -DJPEGXL_ENABLE_SJPEG=OFF -DJPEGXL_ENABLE_OPENEXR=OFF \
    -DJPEGXL_ENABLE_LTO=OFF -DJPEGXL_ENABLE_SKCMS=ON \
    -DJPEGXL_ENABLE_TRANSCODE_JPEG=OFF -DJPEGXL_ENABLE_BOXES=OFF \
    > "$report/configure-$abi.log" 2>&1
  /usr/bin/time -f 'wall_seconds=%e\nmaxrss_kib=%M' -o "$report/build-metrics-$abi.txt" \
    timeout 600s cmake --build "$folder" --target jxl jxl_dec jxl_threads --parallel 2 \
    > "$report/build-$abi.log" 2>&1
  printf 'Build complete; inspecting %s without declaring Android runtime acceptance\n' "$abi"
done
python3 "$(dirname "$0")/inspect-jxl-libraries.py" "$root" "$JXL_REF" "$ABI_REQUEST"
