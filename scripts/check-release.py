#!/usr/bin/env python3
"""Check source release identity; optionally render an explicitly unpublished notes draft.

No network, SDK, signing, package inspection, upload or publication is performed.
"""
import argparse
import json
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
APP_ID = "org.cajapersonal.fixmyshot"
NUMBER = r"(?:0|[1-9][0-9]*)"
VERSION = re.compile(rf"{NUMBER}\.{NUMBER}\.{NUMBER}(?:-([0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*))?")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def validate(root=ROOT):
    ledger = json.loads((root / "docs/releases/ledger.json").read_text())
    require(ledger.get("schema") == 1 and ledger.get("application_id") == APP_ID,
            "Unexpected ledger schema or application ID")
    releases = ledger.get("releases")
    require(isinstance(releases, list) and bool(releases), "Release ledger is empty")
    require(releases[0].get("version_code") == 1, "The first versionCode must be 1")
    previous = 0
    names, hashes, certificates = set(), set(), set()
    for index, entry in enumerate(releases):
        name, code, status = entry.get("version_name"), entry.get("version_code"), entry.get("status")
        match = VERSION.fullmatch(name) if isinstance(name, str) else None
        require(match is not None, "Version must be SemVer without a leading v or build suffix")
        prerelease = match.group(1)
        require(not prerelease or all(not part.isdigit() or part == "0" or not part.startswith("0")
                for part in prerelease.split(".")), "Numeric prerelease identifiers cannot have leading zeroes")
        require(type(code) is int and code > previous, "versionCode must be positive and strictly increasing")
        require(name not in names, "Duplicate versionName")
        require(status in ("planned", "distributed"), "Unknown ledger status")
        require(status != "planned" or index == len(releases) - 1, "Only the current candidate may be planned")
        if status == "distributed":
            for field, length in (("signed_apk_sha256", 64), ("certificate_sha256", 64), ("source_commit", 40)):
                require(re.fullmatch(rf"[0-9a-f]{{{length}}}", entry.get(field, "")) is not None,
                        f"Distributed entry needs its non-secret {field}")
            require(entry["signed_apk_sha256"] not in hashes, "One APK digest cannot identify different releases")
            hashes.add(entry["signed_apk_sha256"])
            certificates.add(entry["certificate_sha256"])
            require(len(certificates) == 1, "Signing identity changed; explicit lineage review is required")
        previous = code
        names.add(name)
    candidate = releases[-1]
    gradle = (root / "app/build.gradle.kts").read_text()
    for field, expected, pattern in (
        ("applicationId", APP_ID, r'applicationId\s*=\s*"([^"]+)"'),
        ("versionName", candidate["version_name"], r'versionName\s*=\s*"([^"]+)"'),
        ("versionCode", str(candidate["version_code"]), r'versionCode\s*=\s*([0-9]+)\b'),
    ):
        require(re.findall(pattern, gradle) == [expected], f"Ledger/Gradle mismatch: {field}")
    name = candidate["version_name"]
    require(name in (root / "README.md").read_text(), "README must identify the current version")
    changelog = (root / "CHANGELOG.md").read_text()
    heading = f"## Unreleased — {name}" if candidate["status"] == "planned" else f"## {name}"
    require(heading in changelog.splitlines(), "Changelog/current ledger status mismatch")
    template = (root / "docs/releases/TEMPLATE.md").read_text()
    require(set(re.findall(r"\{\{([^{}]+)\}\}", template)) == {"VERSION", "TAG", "CODE", "APK"},
            "Unexpected or missing release-template fields")
    apk = f"FixMyShot-{name}-universal.apk"
    require(re.fullmatch(r"FixMyShot-.*-universal\.apk", apk) is not None, "Obtainium filter mismatch")
    return candidate, template, {"VERSION": name, "TAG": f"v{name}", "CODE": str(candidate["version_code"]), "APK": apk}


def render_notes(template, fields, output):
    for key, value in fields.items():
        template = template.replace("{{" + key + "}}", value)
    # Exclusive creation avoids replacing accepted notes or an existing deliverable.
    with output.open("x", encoding="utf-8") as stream:
        stream.write(template)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--notes-output", type=Path, help="Create a NEW unpublished Markdown draft; never overwrite")
    args = parser.parse_args()
    try:
        candidate, template, fields = validate()
        if args.notes_output:
            require(candidate["status"] == "planned", "Notes rendering is for the planned candidate only")
            render_notes(template, fields, args.notes_output)
        print(f"PASS: source metadata only — {fields['VERSION']} / code {fields['CODE']}; APK/signature/upgrade NOT RUN")
    except (OSError, ValueError, KeyError, TypeError, AttributeError) as error:
        print(f"FAIL: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
