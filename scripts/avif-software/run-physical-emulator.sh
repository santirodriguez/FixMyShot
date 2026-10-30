#!/usr/bin/env bash
# Disposable emulator: only an in-app deterministic 1344x2992 synthetic fixture.
set -euo pipefail
mkdir -p evidence/physical-api36
pkg='org.cajapersonal.fixmyshot.avifphysical'
adb install -r candidate/FixMyShot-AVIF-Physical-debug.apk
adb shell am start -n "$pkg/.ProbeActivity" --ez synthetic_ci true \
    > evidence/physical-api36/activity-start.txt
for attempt in $(seq 1 110); do
    if adb exec-out run-as "$pkg" cat files/physical_ci_report.json \
        > evidence/physical-api36/report.json 2>/dev/null &&
        [[ -s evidence/physical-api36/report.json ]]; then
        break
    fi
    sleep 2
done
test -s evidence/physical-api36/report.json || {
    echo 'Physical diagnostic never completed on emulator' >&2
    adb logcat -d -t 200 | grep -E 'FATAL EXCEPTION|avifphysical' | tail -n 25 >&2 || true
    exit 1
}
adb exec-out run-as "$pkg" cat cache/physical_ci_0.avif \
    > evidence/physical-api36/fixture-1344x2992.avif
adb shell getconf PAGESIZE > evidence/physical-api36/page-size.txt
python3 - <<'PY'
import json
from pathlib import Path
report=json.loads(Path('evidence/physical-api36/report.json').read_text())
assert report['kind']=='ISOLATED_PHYSICAL_AVIF_R1__NOT_PRODUCTION', report
assert report['input_kind']=='synthetic_ci', report
assert report['input_size']=='1344x2992', report
assert report['original_image_in_report'] is False, report
assert report['device_decode_status']=='OK', report
assert len(report['trials'])==1, report
trial=report['trials'][0]
assert trial['encode_ns']>0 and trial['output_bytes']>20, trial
assert trial['concurrent_memory']['valid_samples']>=1, trial
assert trial['concurrent_memory']['max_pss_kib']>0, trial
quality=report['quality']
assert quality['psnr_db']>=38.0, quality
assert quality['max_channel_error']<=2, quality
assert quality['nonopaque_pixels']==0, quality
PY
ffprobe -v error -select_streams v:0 -show_entries stream=width,height,pix_fmt,color_range \
    -of json evidence/physical-api36/fixture-1344x2992.avif \
    > evidence/physical-api36/host-decode.json
python3 - <<'PY'
import json
from pathlib import Path
data=json.loads(Path('evidence/physical-api36/host-decode.json').read_text())['streams'][0]
assert (data['width'],data['height'])==(1344,2992),data
assert data['pix_fmt']=='yuv444p' and data['color_range']=='pc',data
PY
sha256sum evidence/physical-api36/fixture-1344x2992.avif \
    evidence/physical-api36/report.json > evidence/physical-api36/sha256.txt
