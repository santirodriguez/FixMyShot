#!/usr/bin/env bash
# Disposable emulator: only an in-app deterministic 1344x2992 synthetic fixture.
set -euo pipefail
mkdir -p evidence/physical-api36
pkg='org.cajapersonal.fixmyshot.avifphysical'
adb install -r candidate/FixMyShot-AVIF-Physical-debug.apk
adb shell am start -n "$pkg/.ProbeActivity" --ez synthetic_ci true \
    > evidence/physical-api36/activity-start.txt
# adb exec-out may report a remote read error on stdout while exiting zero.
# Check the actual private file and parse a complete JSON report before accepting it.
report_ready=0
for attempt in $(seq 1 110); do
    if adb shell run-as "$pkg" test -s files/physical_ci_report.json \
        >/dev/null 2>&1 &&
       adb exec-out run-as "$pkg" cat files/physical_ci_report.json \
        > evidence/physical-api36/report.candidate.json 2>/dev/null &&
       python3 - evidence/physical-api36/report.candidate.json <<'CHECK_REPORT'
import json
import sys
try:
    with open(sys.argv[1], encoding="utf-8") as file:
        report = json.load(file)
    valid = (
        report.get("kind") == "ISOLATED_PHYSICAL_AVIF_R1__NOT_PRODUCTION"
        and report.get("input_kind") == "synthetic_ci"
        and isinstance(report.get("trials"), list)
        and len(report["trials"]) == 1
    )
except (OSError, ValueError, TypeError, AttributeError):
    valid = False
sys.exit(0 if valid else 1)
CHECK_REPORT
    then
        mv evidence/physical-api36/report.candidate.json \
           evidence/physical-api36/report.json
        report_ready=1
        break
    fi
    sleep 2
done
if [[ "$report_ready" -ne 1 ]]; then
    echo 'No complete synthetic JSON report after bounded emulator polling' >&2
    adb logcat -d -t 400 > evidence/physical-api36/timeout-logcat.txt || true
    grep -E 'FATAL EXCEPTION|avifphysical|AndroidRuntime' \
        evidence/physical-api36/timeout-logcat.txt | tail -n 30 >&2 || true
    exit 1
fi
if ! adb shell run-as "$pkg" test -s cache/physical_ci_0.avif \
    >/dev/null 2>&1; then
    echo 'No nonempty app-private synthetic AVIF fixture' >&2
    exit 1
fi
adb exec-out run-as "$pkg" cat cache/physical_ci_0.avif \
    > evidence/physical-api36/fixture-1344x2992.avif
test -s evidence/physical-api36/fixture-1344x2992.avif
adb shell getconf PAGESIZE > evidence/physical-api36/page-size.txt
# Record every decoder's result and error locations before enforcing fidelity.
# A red Android path remains red even if independent host decode looks better.
python3 - <<'PY'
import json
from pathlib import Path
report=json.loads(Path('evidence/physical-api36/report.json').read_text())
assert report['kind']=='ISOLATED_PHYSICAL_AVIF_R1__NOT_PRODUCTION'
assert report['input_kind']=='synthetic_ci'
assert report['input_size']=='1344x2992'
assert report['original_image_in_report'] is False
assert len(report['trials'])==1
trial=report['trials'][0]
assert trial['encode_ns']>0 and trial['output_bytes']>20, trial
assert trial['concurrent_memory']['valid_samples']>=1, trial
assert trial['concurrent_memory']['max_pss_kib']>0, trial
expected_modes=[
    'bitmap_factory_default',
    'bitmap_factory_explicit_srgb',
    'image_decoder_srgb_software',
]
modes=report['decoder_paths']
assert isinstance(modes,list) and [m['mode'] for m in modes]==expected_modes, modes
summary={'kind':'ISOLATED_ANDROID_AVIF_DECODER_MATRIX_NOT_PRODUCTION',
         'source':'single shared synthetic AVIF file',
         'display_fidelity_gate':'PSNR>=38, max per-channel error<=2, opaque and sRGB',
         'paths':[], 'all_display_paths_pass':False}
for mode in modes:
    assert mode['status'] in (
        'OK','WRONG_GEOMETRY','NOT_SRGB','UNAVAILABLE_OR_FAILED','DECODE_EXCEPTION'),mode
    row={'mode':mode['mode'], 'status':mode['status'], 'pass':False}
    if mode['status']=='OK':
        assert (mode['width'],mode['height'])==(1344,2992),mode
        q=mode['quality']
        pixels=1344*2992
        bins=q['max_channel_error_histogram_0_1_2_3to7_8to15_16to31_32plus']
        assert len(bins)==7 and all(isinstance(i,int) and i>=0 for i in bins),q
        assert sum(bins)==pixels,q
        assert sum(bins[3:])==q['pixels_error_gt_2'],q
        assert bins[6]<=q['pixels_error_gt_16']<=bins[5]+bins[6],q
        grid=q['pixel_error_gt_2_grid_4x6']
        assert len(grid)==4 and all(len(row)==6 for row in grid),q
        assert sum(sum(row) for row in grid)==q['pixels_error_gt_2'],q
        assert len(q['channel_error_gt_2_rgb'])==3,q
        assert 0 <= q['max_channel_error'] <= 255,q
        row.update({'color_space':mode['color_space'],'psnr_db':q['psnr_db'],
                    'max_channel_error':q['max_channel_error'],
                    'pixels_error_gt_2':q['pixels_error_gt_2'],
                    'pixels_error_gt_16':q['pixels_error_gt_16'],
                    'worst_error_x':q['worst_error_x'],
                    'worst_error_y':q['worst_error_y'],
                    'histogram':bins,'grid_4x6':grid,
                    'channel_error_gt_2_rgb':q['channel_error_gt_2_rgb']})
        row['pass']=(q['psnr_db']>=38.0 and q['max_channel_error']<=2
                     and q['nonopaque_pixels']==0 and
                     q['pixels_error_gt_2']==0)
    else:
        row['failure_class']=mode.get('exception_type',mode['status'])
    summary['paths'].append(row)
assert report['device_decode_status']==modes[0]['status']
if modes[0]['status']=='OK':
    assert report['quality']==modes[0]['quality'], 'Default preview quality changed'
summary['all_display_paths_pass']=all(r['pass'] for r in summary['paths'])
summary['pixel_approval_for_physical_handoff']=False  # no physical test was executed
out=Path('evidence/physical-api36/decoder-comparison.json')
out.write_text(json.dumps(summary,sort_keys=True,indent=2)+'\n')
for r in summary['paths']:
    print('Decoder:',r['mode'],'status=',r['status'],'gate_pass=',r['pass'],
          'max_error=',r.get('max_channel_error'),
          'pixels_gt2=',r.get('pixels_error_gt_2'))
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
    evidence/physical-api36/report.json \
    evidence/physical-api36/decoder-comparison.json \
    > evidence/physical-api36/sha256.txt
# This diagnostic is intentionally strict: do NOT turn a receiver/display
# discrepancy into green CI merely because another decoder passes.
python3 - <<'PY'
import json
from pathlib import Path
result=json.loads(Path('evidence/physical-api36/decoder-comparison.json').read_text())
if not result['all_display_paths_pass']:
    raise SystemExit('Android display decoder gate FAILED; comparative metrics preserved as CI artifact')
print('All three Android decoder paths passed the unchanged fidelity thresholds')
PY
