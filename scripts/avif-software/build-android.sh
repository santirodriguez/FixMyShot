#!/usr/bin/env bash
# Standalone Android ABI source-built sample; never modifies an application build.
set -euo pipefail
if [[ $# -ne 2 ]]; then echo 'usage: build-android.sh <fresh-output-directory> <abi>' >&2; exit 2; fi
output="$1"
abi="$2"
case "$abi" in arm64-v8a|armeabi-v7a|x86_64) ;; *) echo 'Unsupported ABI' >&2; exit 2;; esac
if [[ -e "$output" ]]; then echo 'Output already exists (refusing overwrite)' >&2; exit 2; fi
if [[ -z "${ANDROID_NDK_ROOT:-}" || ! -f "$ANDROID_NDK_ROOT/build/cmake/android.toolchain.cmake" ]]; then
    echo 'Missing preinstalled Android NDK' >&2; exit 2
fi
if ! grep -qx 'Pkg.Revision = 28.2.13676358' "$ANDROID_NDK_ROOT/source.properties"; then
    echo 'NDK revision does not match FixMyShot pinned toolchain' >&2; exit 2
fi
mkdir -p "$output"/sources "$output"/evidence
output="$(cd "$output" && pwd)"

# Upstream release tags are checked against immutable source commit IDs.
# Fail closed on a tag rewrite, network error or source/ABI mismatch.
git clone --depth 1 --branch v1.4.2 https://github.com/AOMediaCodec/libavif.git "$output/sources/libavif"
libavif_sha="$(git -C "$output/sources/libavif" rev-parse HEAD)"
[[ "$libavif_sha" == c5240fc79fe5c2407e10afd35f5505ef6333ea49 ]] || { echo 'Unexpected libavif source' >&2; exit 1; }
git clone --depth 1 --branch v3.15.2 https://aomedia.googlesource.com/aom.git "$output/sources/libavif/ext/aom"
libaom_sha="$(git -C "$output/sources/libavif/ext/aom" rev-parse HEAD)"
[[ "$libaom_sha" == af3dc9aadc793c1b00edc003e3fc19a44fdeb574 ]] || { echo 'Unexpected libaom source' >&2; exit 1; }

cmake -S "$(dirname "$(realpath "$0")")" -B "$output/build" -G Ninja \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_ROOT/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$abi" -DANDROID_PLATFORM=android-26 -DCMAKE_BUILD_TYPE=Release \
    -DLIBAVIF_SOURCE_ROOT="$output/sources/libavif" \
    -DCMAKE_EXPORT_COMPILE_COMMANDS=ON \
    2>&1 | tee "$output/evidence/configure.log"
cmake --build "$output/build" --target avif_spike_cli --parallel 3 \
    2>&1 | tee "$output/evidence/build.log"
cp "$output/build/avif_spike_cli" "$output/evidence/avif_spike_cli"
"$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf" -l \
    "$output/evidence/avif_spike_cli" > "$output/evidence/program-headers.txt"
(cd "$output/evidence" && sha256sum avif_spike_cli > sha256.txt)
printf 'libavif_sha=%s\nlibaom_sha=%s\nabi=%s\nndk=%s\n' \
    "$libavif_sha" "$libaom_sha" "$abi" 28.2.13676358 \
    > "$output/evidence/source-versions.txt"
# A compressed CLI estimate is not an APK delta or a shipping .so size.
python3 - "$output/evidence/avif_spike_cli" "$output/evidence/size.json" <<'PY'
import json, pathlib, sys, zlib
p = pathlib.Path(sys.argv[1])
b = p.read_bytes()
pathlib.Path(sys.argv[2]).write_text(json.dumps({
    'kind': 'ANDROID_EXECUTABLE_SIZE_ONLY__NOT_APK_DELTA',
    'raw_bytes': len(b), 'deflate_bytes': len(zlib.compress(b, 9)),
}, indent=2)+'\n')
PY
