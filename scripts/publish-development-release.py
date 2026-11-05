#!/usr/bin/env python3
"""Verify a prepared kit and publish only through an explicit main-branch dispatch."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
from urllib.parse import quote

ROOT = Path(__file__).resolve().parents[1]
REPO = "santirodriguez/FixMyShot"
API = "repos/" + REPO


def require(ok, message):
    if not ok:
        raise ValueError(message)


def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


class GitHub:
    """Use the runner's GitHub CLI; no token arguments, persistence or custom redirects."""
    def json(self, method, path, value=None, missing=False):
        args = ["gh", "api", "--method", method, path,
                "-H", "X-GitHub-Api-Version: 2022-11-28"]
        if value is not None:
            args += ["--input", "-"]
        result = subprocess.run(args, input=None if value is None else json.dumps(value).encode(),
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
        if missing and result.returncode and b"(HTTP 404)" in result.stderr:
            return None
        require(result.returncode == 0, "GitHub API operation failed: " + method + " " + path)
        return json.loads(result.stdout)

    def upload(self, release_id, path):
        endpoint = "https://uploads.github.com/" + API + "/releases/" + str(release_id) + "/assets?name=" + quote(path.name)
        result = subprocess.run(["gh", "api", "--method", "POST", endpoint,
                                 "-H", "Content-Type: application/octet-stream", "--input", str(path)],
                                stdout=subprocess.PIPE, stderr=subprocess.PIPE, check=False)
        require(result.returncode == 0, "Asset upload failed; retain the draft and retry the same kit")
        return json.loads(result.stdout)

    def download(self, asset_id, path):
        with path.open("xb") as stream:
            result = subprocess.run(["gh", "api", API + "/releases/assets/" + str(asset_id),
                                     "-H", "Accept: application/octet-stream"],
                                    stdout=stream, stderr=subprocess.PIPE, check=False)
        require(result.returncode == 0, "Could not independently read back a release asset")


def select_run(client, run_id):
    require(type(run_id) is int and run_id > 0, "Invalid staging run ID")
    run = client.json("GET", API + "/actions/runs/" + str(run_id))
    require(run["conclusion"] == "success" and run["event"] == "push"
            and run["head_branch"] == "release/1.0.0"
            and run["path"] == ".github/workflows/prepare-development-release.yml"
            and run["repository"]["full_name"] == REPO,
            "Use a successful release/1.0.0 push run of the staging workflow")
    control = run["head_sha"]
    require(re.fullmatch(r"[0-9a-f]{40}", control) is not None, "Invalid staging source")
    name = "development-release-kit-" + control
    artifacts = client.json("GET", API + "/actions/runs/" + str(run_id) + "/artifacts?per_page=100")
    matches = [a for a in artifacts["artifacts"] if a["name"] == name and not a["expired"]]
    require(len(matches) == 1, "The exact prepared kit is missing or expired; restage before publication")
    return {"artifact": name, "control": control}


def release_policy(request):
    version = request.get("version_name")
    match = re.fullmatch(r"((?:0|[1-9][0-9]*))\.((?:0|[1-9][0-9]*))\.((?:0|[1-9][0-9]*))(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?", version) if isinstance(version, str) else None
    require(match is not None, "Public version must be valid SemVer")
    core = tuple(int(match.group(i)) for i in (1, 2, 3))
    code = request.get("version_code")
    require(type(code) is int and ((version == "1.0.0" and code == 1)
            or (core > (1, 0, 0) and code > 1)),
            "Public release sequence must begin with 1.0.0/code1 and advance from there")
    prerelease = match.group(4)
    require(not prerelease or all(not part.isdigit() or part == "0" or not part.startswith("0")
            for part in prerelease.split(".")), "Numeric prerelease identifiers cannot have leading zeroes")
    stable = prerelease is None
    require(request.get("release_channel", "stable" if stable else "development-prerelease")
            == ("stable" if stable else "development-prerelease"), "Version/channel mismatch")
    return {"prerelease": not stable, "make_latest": stable}


def verify_kit(kit, control, request):
    require(request.get("state") == "READY", "Release inputs are not ready")
    policy = release_policy(request)
    version = request["version_name"]
    require(request["tag_name"] == "v" + version
            and request["signed_apk"]["filename"] == "FixMyShot-" + version + "-universal.apk"
            and request["source_materials"]["filename"] == "FixMyShot-" + version + "-source-materials.tar.gz",
            "Version/tag/asset names differ")
    require(re.fullmatch(r"[0-9a-f]{40}", control or "") is not None, "Invalid kit control commit")
    plan = json.loads((kit / "review/release-plan.json").read_text())
    receipt = json.loads((kit / "review/staging-receipt.json").read_text())
    info = json.loads((kit / "assets/BUILD-INFO.json").read_text())
    tag = request["tag_name"]
    require(plan["prerelease"] is policy["prerelease"] and plan["make_latest"] is policy["make_latest"]
            and plan["tag_name"] == tag and plan["proposed_tag_target_commit"] == control
            and receipt["control_commit"] == control
            and info["proposed_tag_target_commit"] == control, "Kit tag/control differs")
    require(plan["compiled_source_commit"] == request["compiled_source"]
            and info["compiled_source_commit"] == request["compiled_source"]
            and info["compiled_source_tree"] == request["compiled_tree"]
            and info["application_id"] == request["application_id"]
            and info["version_name"] == request["version_name"]
            and info["version_code"] == request["version_code"], "Compiled identity differs")
    records = plan["assets"]
    expected = {request["signed_apk"]["filename"], request["source_materials"]["filename"],
                "BUILD-INFO.json", "SHA256SUMS.txt"}
    require(len(records) == 4 and {r["filename"] for r in records} == expected
            and records == receipt["assets"], "Expected exactly four reviewed assets")
    actual_paths = list((kit / "assets").iterdir())
    require({p.name for p in actual_paths} == expected
            and all(p.is_file() and not p.is_symlink() for p in actual_paths), "Unexpected asset files")
    for record in records:
        path = kit / "assets" / record["filename"]
        require(path.stat().st_size == record["bytes"] and sha256(path) == record["sha256"], "Asset digest differs")
    for key in ("signed_apk", "source_materials"):
        pinned = request[key]
        actual = next(r for r in records if r["filename"] == pinned["filename"])
        require(all(actual[k] == pinned[k] for k in ("bytes", "sha256")), "Frozen input changed")
    require(info["signed_apk"] == request["signed_apk"], "Signing receipt differs")
    sums = "".join(r["sha256"] + "  " + r["filename"] + "\n"
                   for r in sorted(records, key=lambda r: r["filename"]) if r["filename"] != "SHA256SUMS.txt")
    require((kit / "assets/SHA256SUMS.txt").read_text() == sums, "Checksum coverage differs")
    notes = (kit / "review/RELEASE-NOTES.md").read_text()
    require("{{" not in notes and "<!-- EDIT" not in notes and "Caja Personal" in notes
            and control in notes, "Release notes are unresolved")
    require(plan["coverage_limits"] == info["coverage_limits"], "Coverage limits differ")
    if policy["prerelease"]:
        require(plan["coverage_limits"]["functional_acceptance"] == "INCOMPLETE"
                and "incomplete" in notes, "Development coverage changed")
    else:
        require(info["coverage_limits"].get("functional_acceptance") == "ACCEPTED"
                and request.get("stable_acceptance_reviewed") is True,
                "Stable acceptance has not been reviewed")
    return plan, notes


def publish(client, kit, control, main, request, confirmation):
    plan, notes = verify_kit(kit, control, request)
    policy = release_policy(request)
    tag = plan["tag_name"]
    require(confirmation == "PUBLISH " + tag, "Exact publication confirmation required")
    require(re.fullmatch(r"[0-9a-f]{40}", main or "") is not None, "Invalid main checkout")

    def check_main():
        require(client.json("GET", API + "/git/ref/heads/main")["object"]["sha"] == main,
                "Main advanced; review the new source before publication")

    check_main()
    ancestry = client.json("GET", API + "/compare/" + control + "..." + main)
    require(ancestry["status"] in ("ahead", "identical")
            and ancestry["merge_base_commit"]["sha"] == control, "Kit control is not merged into main")
    ref_path = API + "/git/ref/tags/" + quote(tag, safe="")
    ref = client.json("GET", ref_path, missing=True)
    require(ref is None or (ref["object"]["type"] == "commit" and ref["object"]["sha"] == control),
            "Existing tag points elsewhere; never move it")
    releases = client.json("GET", API + "/releases?per_page=100")
    matches = [r for r in releases if r["tag_name"] == tag]
    require(len(matches) <= 1, "Ambiguous release state")
    release = matches[0] if matches else None
    if release:
        require(release["name"] == "FixMyShot " + request["version_name"]
                and release["body"] == notes
                and release["prerelease"] is policy["prerelease"], "Existing release differs; reconcile manually")
    if ref is None:
        require(release is None, "Release exists without its expected tag")
        client.json("POST", API + "/git/refs", {"ref": "refs/tags/" + tag, "sha": control})
    if release is None:
        release = client.json("POST", API + "/releases", {
            "tag_name": tag, "target_commitish": control,
            "name": "FixMyShot " + request["version_name"], "body": notes,
            "draft": True, "prerelease": policy["prerelease"], "make_latest": str(policy["make_latest"]).lower()})
    records = {r["filename"]: r for r in plan["assets"]}

    def check_assets(allow_upload):
        assets = client.json("GET", API + "/releases/" + str(release["id"]) + "/assets?per_page=100")
        require(len({a["name"] for a in assets}) == len(assets)
                and all(a["name"] in records for a in assets), "Unexpected remote assets; never overwrite them")
        by_name = {a["name"]: a for a in assets}
        for name, record in records.items():
            if name not in by_name:
                require(allow_upload, "Published release is missing an asset")
                by_name[name] = client.upload(release["id"], kit / "assets" / name)
            asset = by_name[name]
            require(asset["size"] == record["bytes"] and asset["state"] == "uploaded", "Remote asset size/state differs")
            with tempfile.TemporaryDirectory() as folder:
                downloaded = Path(folder) / name
                client.download(asset["id"], downloaded)
                require(downloaded.stat().st_size == record["bytes"] and sha256(downloaded) == record["sha256"],
                        "Remote asset readback differs; release remains a draft")
        return {a["id"]: (a["name"], a["size"], a["state"]) for a in by_name.values()}

    verified_assets = check_assets(release["draft"])
    check_main()
    actual_tag = client.json("GET", ref_path)["object"]
    require(actual_tag["type"] == "commit" and actual_tag["sha"] == control, "Tag changed during upload")
    current = client.json("GET", API + "/releases/" + str(release["id"]))
    require(current["name"] == release["name"] and current["body"] == notes
            and current["draft"] == release["draft"] and current["prerelease"] is policy["prerelease"],
            "Release changed during upload; inspect it before retrying")
    assets = client.json("GET", API + "/releases/" + str(release["id"]) + "/assets?per_page=100")
    require(len(assets) == 4 and {a["id"]: (a["name"], a["size"], a["state"]) for a in assets} == verified_assets,
            "Assets changed after verification; release remains a draft")
    if release["draft"]:
        release = client.json("PATCH", API + "/releases/" + str(release["id"]),
                              {"draft": False, "prerelease": policy["prerelease"], "make_latest": str(policy["make_latest"]).lower()})
    require(release["draft"] is False and release["prerelease"] is policy["prerelease"], "Publication readback failed")
    return {"release_url": release["html_url"], "tag": tag, "tag_target": control,
            "assets_verified": 4, "obtainium_device_acceptance": "NOT_RUN"}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--select-run", type=int)
    parser.add_argument("--kit", type=Path)
    parser.add_argument("--control")
    parser.add_argument("--publish", action="store_true")
    args = parser.parse_args()
    require(os.environ.get("GITHUB_REPOSITORY") == REPO, "Unexpected repository")
    if args.select_run:
        selected = select_run(GitHub(), args.select_run)
        with open(os.environ["GITHUB_OUTPUT"], "a") as stream:
            stream.write("\n".join(k + "=" + v for k, v in selected.items()) + "\n")
    else:
        request = json.loads((ROOT / ".ci/development-release-request.json").read_text())
        if args.publish:
            require(os.environ.get("GITHUB_EVENT_NAME") == "workflow_dispatch"
                    and os.environ.get("GITHUB_REF") == "refs/heads/main", "Publish only by manual dispatch on main")
            result = publish(GitHub(), args.kit, args.control, os.environ.get("GITHUB_SHA"),
                             request, os.environ.get("RELEASE_CONFIRMATION"))
            print(json.dumps(result, indent=2))
        else:
            verify_kit(args.kit, args.control, request)
            print("PASS: exact prepared kit; no tag or release operation")
