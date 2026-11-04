#!/usr/bin/env bash
# Compatibility entry point: validate the committed wrapper without regenerating it.
set -euo pipefail
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
python3 "$root/scripts/check-gradle-wrapper.py" "$root"
