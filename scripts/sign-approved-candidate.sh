#!/usr/bin/env bash
# Sign only the source/run/hash-bound candidate. Never execute downloaded artifact code.
set -euo pipefail
set +x
umask 077
: "${ANDROID_HOME:?Android SDK is required}"
: "${RUNNER_TEMP:?Runner temporary directory is required}"
: "${FIXMYSHOT_KEYSTORE_BASE64:?Missing app keystore secret}"
: "${FIXMYSHOT_KEYSTORE_PASSWORD:?Missing app keystore password}"
tools="$ANDROID_HOME/build-tools/36.0.0"
eval "$(python3 - <<'PY'
import json,re,shlex
from pathlib import Path
r=json.loads(Path('.ci/signing-candidate-request.json').read_text())
if r.get('state') != 'SIGN_CANDIDATE': raise SystemExit('Signing is not enabled')
for key,pattern in [('unsigned_sha256',r'[0-9a-f]{64}'),('source_commit',r'[0-9a-f]{40}')]:
    if re.fullmatch(pattern,r.get(key,'')) is None: raise SystemExit('Missing exact signing pin')
if r.get('version_name') != '1.0.0' or r.get('version_code') != 1 or type(r.get('unsigned_bytes')) is not int or not 0 < r['unsigned_bytes'] <= 40*1024*1024:
    raise SystemExit('Unexpected first-public candidate')
for key,value in {'expected_apk':r['unsigned_sha256'],'expected_bytes':r['unsigned_bytes']}.items():
    print(key+'='+shlex.quote(str(value)))
PY
)"
expected_cert=33e626f0980ffd263e24e688a372ff840fd877f400f2a9ea5db5759655bbf445
mapfile -d '' candidates < <(find candidate-input -type f -name app-release-unsigned.apk -print0)
[[ ${#candidates[@]} == 1 ]] || { echo 'Expected exactly one unsigned release APK' >&2; exit 1; }
apk="${candidates[0]}"
[[ "$(stat -c %s "$apk")" == "$expected_bytes" ]]
[[ "$(sha256sum "$apk" | cut -d ' ' -f 1)" == "$expected_apk" ]]
check_identity() {
  "$tools/aapt" dump badging "$1" | python3 -c '
import re,sys
line=sys.stdin.readline().strip()
fields=dict(re.findall(r"(\w+)=\x27([^\x27]*)\x27", line))
if not line.startswith("package: ") or any(fields.get(k) != v for k,v in {
"name":"org.cajapersonal.fixmyshot", "versionCode":"1", "versionName":"1.0.0"}.items()):
    raise SystemExit("Unexpected APK identity")
sys.stdin.read()
'
}
check_identity "$apk"
"$tools/zipalign" -c -P 16 4 "$apk"
test ! -e candidate-output
mkdir candidate-output
key_dir="$(mktemp -d "$RUNNER_TEMP/fixmyshot-signing.XXXXXX")"
trap 'rm -rf -- "$key_dir"' EXIT
printf '%s' "$FIXMYSHOT_KEYSTORE_BASE64" | base64 --decode > "$key_dir/app.p12"
unset FIXMYSHOT_KEYSTORE_BASE64
signed=candidate-output/FixMyShot-1.0.0-universal.apk
"$tools/apksigner" sign --ks "$key_dir/app.p12" --ks-type PKCS12 \
  --ks-key-alias fixmyshot-app-signing \
  --ks-pass env:FIXMYSHOT_KEYSTORE_PASSWORD --key-pass env:FIXMYSHOT_KEYSTORE_PASSWORD \
  --min-sdk-version 26 --v4-signing-enabled false --out "$signed" "$apk"
unset FIXMYSHOT_KEYSTORE_PASSWORD
"$tools/apksigner" verify --verbose --print-certs --min-sdk-version 26 "$signed" > "$key_dir/signature.txt"
grep -Fx "Signer #1 certificate SHA-256 digest: $expected_cert" "$key_dir/signature.txt"
[[ "$(grep -c '^Signer #[0-9]* certificate SHA-256 digest:' "$key_dir/signature.txt")" == 1 ]]
"$tools/zipalign" -c -P 16 4 "$signed"
check_identity "$signed"
cp "$key_dir/signature.txt" candidate-output/signature-verification.txt
sha256sum "$signed" > candidate-output/SHA256SUMS
python3 - "$signed" "$expected_apk" "$expected_cert" <<'PY'
import hashlib,json,os,sys
from pathlib import Path
apk=Path(sys.argv[1])
request=json.loads(Path(".ci/signing-candidate-request.json").read_text())
receipt={"schema":1,"status":"SIGNED_AND_VERIFIED__NOT_FUNCTIONAL_RELEASE_ACCEPTANCE",
"source_commit":request["source_commit"],
"unsigned_apk_sha256":sys.argv[2],"signed_apk_sha256":hashlib.sha256(apk.read_bytes()).hexdigest(),
"signed_apk_bytes":apk.stat().st_size,"certificate_sha256":sys.argv[3],
"package":"org.cajapersonal.fixmyshot","version_name":"1.0.0","version_code":1,
"minimum_api_verified":26,"build_tools":"36.0.0","workflow_commit":os.environ["GITHUB_SHA"],
"workflow_run_id":os.environ["GITHUB_RUN_ID"],"physical_install_test":"NOT_RUN",
"sequential_upgrade_test":"NOT_RUN","obtainium_test":"NOT_RUN"}
Path("candidate-output/BUILD-INFO.json").write_text(json.dumps(receipt,indent=2)+"\n")
PY
