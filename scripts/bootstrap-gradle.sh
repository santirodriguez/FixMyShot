#!/usr/bin/env bash
# Temporary Phase 1 bootstrap. Generates the standard wrapper without resolving app dependencies.
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cache="$root/.gradle-bootstrap"
version=8.13
archive_sha=20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78
wrapper_sha=81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f
mkdir -p "$cache"
archive="$cache/gradle-$version-bin.zip"
if [[ ! -f "$archive" ]]; then
  curl --fail --location --retry 2 --proto '=https' --tlsv1.2 \
    "https://services.gradle.org/distributions/gradle-$version-bin.zip" -o "$archive.part"
  mv "$archive.part" "$archive"
fi
printf '%s  %s\n' "$archive_sha" "$archive" | sha256sum --check --status
# Re-extract verified bytes rather than trusting a previous executable cache.
unzip -q -o "$archive" -d "$cache"
work="$(mktemp -d "$cache/wrapper.XXXXXX")"
trap 'rm -rf "$work"' EXIT
printf 'rootProject.name = "wrapper-bootstrap"\n' > "$work/settings.gradle.kts"
"$cache/gradle-$version/bin/gradle" --no-daemon -p "$work" wrapper \
  --gradle-version "$version" --distribution-type bin --gradle-distribution-sha256-sum "$archive_sha"
printf '%s  %s\n' "$wrapper_sha" "$work/gradle/wrapper/gradle-wrapper.jar" | sha256sum --check --status
mkdir -p "$root/gradle/wrapper"
for file in gradlew gradlew.bat gradle/wrapper/gradle-wrapper.jar gradle/wrapper/gradle-wrapper.properties; do
  if [[ -f "$root/$file" ]]; then
    cmp --silent "$work/$file" "$root/$file" || { echo "Refusing to overwrite a different wrapper: $file" >&2; exit 1; }
  else
    cp "$work/$file" "$root/$file"
  fi
done
chmod +x "$root/gradlew"
echo 'Verified standard Gradle 8.13 wrapper generated; review and commit it before freezing G1.'
