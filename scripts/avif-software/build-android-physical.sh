#!/usr/bin/env bash
# Opt-in physical diagnostic. Rebuild exact sources; no production APK changes.
set -euo pipefail
if [[ $# -ne 2 ]]; then
    echo 'usage: build-android-physical.sh <fresh-output-directory> <abi>' >&2
    exit 2
fi
output="$1"
abi="$2"
case "$abi" in arm64-v8a|x86_64) ;; *) echo 'Only physical Pixel ARM64 and API36 emulator x86_64 are in scope' >&2; exit 2;; esac
if [[ -e "$output" ]]; then echo 'Output directory exists' >&2; exit 2; fi
if [[ -z "$ANDROID_NDK_ROOT" || ! -f "$ANDROID_NDK_ROOT/build/cmake/android.toolchain.cmake" ]]; then
    echo 'Preinstalled Android NDK required' >&2; exit 2
fi
grep -qx 'Pkg.Revision = 28.2.13676358' "$ANDROID_NDK_ROOT/source.properties" || {
    echo 'Unexpected NDK source revision' >&2; exit 2;
}
mkdir -p "$output/sources" "$output/evidence/LICENSES"
output="$(cd "$output" && pwd)"
git clone --depth 1 --branch v1.4.2 https://github.com/AOMediaCodec/libavif.git "$output/sources/libavif"
libavif_sha="$(git -C "$output/sources/libavif" rev-parse HEAD)"
[[ "$libavif_sha" == c5240fc79fe5c2407e10afd35f5505ef6333ea49 ]] || {
    echo 'Untrusted libavif revision' >&2; exit 1;
}
git clone --depth 1 --branch v3.15.2 https://aomedia.googlesource.com/aom.git "$output/sources/libavif/ext/aom"
libaom_sha="$(git -C "$output/sources/libavif/ext/aom" rev-parse HEAD)"
[[ "$libaom_sha" == af3dc9aadc793c1b00edc003e3fc19a44fdeb574 ]] || {
    echo 'Untrusted libaom revision' >&2; exit 1;
}
cp "$output/sources/libavif/LICENSE" "$output/evidence/LICENSES/libavif-LICENSE"
cp "$output/sources/libavif/ext/aom/LICENSE" "$output/evidence/LICENSES/libaom-LICENSE"
cp "$output/sources/libavif/ext/aom/PATENTS" "$output/evidence/LICENSES/libaom-PATENTS"
for item in "$output"/evidence/LICENSES/*; do test -s "$item"; done
cmake -S "$(dirname "$(realpath "$0")")" -B "$output/build" -G Ninja \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_ROOT/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$abi" -DANDROID_PLATFORM=android-31 \
    -DCMAKE_BUILD_TYPE=MinSizeRel -DFIXMYSHOT_AVIF_PHYSICAL_PROBE=ON \
    -DFIXMYSHOT_AVIF_DIRECT_DECODE=ON \
    -DLIBAVIF_SOURCE_ROOT="$output/sources/libavif" \
    2>&1 | tee "$output/evidence/configure.log"
cmake --build "$output/build" --target avif_physical --parallel 3 \
    2>&1 | tee "$output/evidence/build.log"
cp "$output/build/libavif_physical.so" "$output/evidence/libavif_physical.so"
readelf="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
"$readelf" -l -d "$output/evidence/libavif_physical.so" > "$output/evidence/elf.txt"
(cd "$output/evidence" && sha256sum libavif_physical.so LICENSES/* > sha256.txt)
printf 'libavif_sha=%s\nlibaom_sha=%s\nabi=%s\nndk=%s\naom_decode=ON\n' \
    "$libavif_sha" "$libaom_sha" "$abi" 28.2.13676358 \
    > "$output/evidence/source-versions.txt"
