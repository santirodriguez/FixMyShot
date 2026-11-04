#!/usr/bin/env bash
# CI-only SDK preparation; reuse the runner's installed command-line tools.
set -euo pipefail
mkdir -p evidence
git rev-parse HEAD > evidence/source-sha.txt
java -version 2> evidence/java-version.txt
sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[[ -n "$sdk_root" && -d "$sdk_root" ]] || { echo 'Runner Android SDK root is unavailable' >&2; exit 1; }
manager=''
for candidate in "$sdk_root/cmdline-tools/latest/bin/sdkmanager" "$sdk_root/cmdline-tools/12.0/bin/sdkmanager"; do
  if [[ -x "$candidate" ]]; then manager="$candidate"; break; fi
done
[[ -n "$manager" ]] || { echo 'Installed Android command-line tools were not found at the supported paths' >&2; exit 1; }
"$manager" --version > evidence/sdk-manager-version.txt
if [[ -n "${GITHUB_PATH:-}" ]]; then
  printf '%s\n' "$(dirname "$manager")" "$sdk_root/platform-tools" "$sdk_root/emulator" >> "$GITHUB_PATH"
fi
"$manager" --sdk_root="$sdk_root" 'platforms;android-36' 'build-tools;36.0.0' 'ndk;28.2.13676358' 'cmake;3.22.1' 2>&1 | tee evidence/sdk-install.txt
