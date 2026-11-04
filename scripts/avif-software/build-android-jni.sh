#!/usr/bin/env bash
# Opt-in three-ABI JNI diagnostic; source pins fail closed and normal APK is untouched.
set -euo pipefail
if [[ $# -ne 2 ]]; then
    echo 'usage: build-android-jni.sh <fresh-output-directory> <abi>' >&2
    exit 2
fi
output="$1"
abi="$2"
case "$abi" in arm64-v8a|armeabi-v7a|x86_64) ;; *) echo 'Unsupported ABI' >&2; exit 2;; esac
if [[ -e "$output" ]]; then echo 'Output already exists' >&2; exit 2; fi
if [[ -z "$ANDROID_NDK_ROOT" ]] || [[ ! -f "$ANDROID_NDK_ROOT/build/cmake/android.toolchain.cmake" ]]; then
    echo 'Preinstalled Android NDK required' >&2; exit 2
fi
grep -qx 'Pkg.Revision = 28.2.13676358' "$ANDROID_NDK_ROOT/source.properties" || {
    echo 'Unexpected NDK revision' >&2; exit 2;
}
mkdir -p "$output/sources" "$output/evidence/LICENSES"
output="$(cd "$output" && pwd)"
git clone --depth 1 --branch v1.4.2 https://github.com/AOMediaCodec/libavif.git "$output/sources/libavif"
libavif_sha="$(git -C "$output/sources/libavif" rev-parse HEAD)"
[[ "$libavif_sha" == c5240fc79fe5c2407e10afd35f5505ef6333ea49 ]] || {
    echo 'Untrusted libavif source revision' >&2; exit 1;
}
git clone --depth 1 --branch v3.15.2 https://aomedia.googlesource.com/aom.git "$output/sources/libavif/ext/aom"
libaom_sha="$(git -C "$output/sources/libavif/ext/aom" rev-parse HEAD)"
[[ "$libaom_sha" == af3dc9aadc793c1b00edc003e3fc19a44fdeb574 ]] || {
    echo 'Untrusted libaom source revision' >&2; exit 1;
}
cp "$output/sources/libavif/LICENSE" "$output/evidence/LICENSES/libavif-LICENSE"
cp "$output/sources/libavif/ext/aom/LICENSE" "$output/evidence/LICENSES/libaom-LICENSE"
cp "$output/sources/libavif/ext/aom/PATENTS" "$output/evidence/LICENSES/libaom-PATENTS"
test -s "$output/evidence/LICENSES/libavif-LICENSE"
test -s "$output/evidence/LICENSES/libaom-LICENSE"
test -s "$output/evidence/LICENSES/libaom-PATENTS"
cmake -S "$(dirname "$(realpath "$0")")" -B "$output/build" -G Ninja \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_ROOT/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$abi" -DANDROID_PLATFORM=android-26 \
    -DCMAKE_BUILD_TYPE=MinSizeRel -DFIXMYSHOT_AVIF_JNI_PROBE=ON \
    -DLIBAVIF_SOURCE_ROOT="$output/sources/libavif" \
    2>&1 | tee "$output/evidence/configure.log"
cmake --build "$output/build" --target avif_probe --parallel 3 \
    2>&1 | tee "$output/evidence/build.log"
cp "$output/build/libavif_probe.so" "$output/evidence/libavif_probe.so"
readelf="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
"$readelf" -l -d "$output/evidence/libavif_probe.so" > "$output/evidence/elf.txt"
(cd "$output/evidence" && sha256sum libavif_probe.so LICENSES/* > sha256.txt)
printf 'libavif_sha=%s\nlibaom_sha=%s\nabi=%s\nndk=%s\n' \
    "$libavif_sha" "$libaom_sha" "$abi" 28.2.13676358 \
    > "$output/evidence/source-versions.txt"
python3 - "$output/evidence/libavif_probe.so" "$output/evidence/size.json" <<'PY'
import json, pathlib, sys, zlib
raw = pathlib.Path(sys.argv[1]).read_bytes()
pathlib.Path(sys.argv[2]).write_text(json.dumps({
    "kind": "STANDALONE_SHARED_OBJECT__NOT_APK_DELTA",
    "so_bytes": len(raw),
    "deflate_bytes": len(zlib.compress(raw, 9)),
}, indent=2) + "\n")
PY
