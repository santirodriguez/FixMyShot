#!/usr/bin/env python3
"""Dependency-free checks for the implemented bootstrap, not future locale completeness."""
from pathlib import Path
import re
import tomllib
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"

def check() -> None:
    resources = ROOT / "app/src/main/res"
    baseline = None
    for qualifier in ("values", "values-es", "values-ca"):
        strings = ET.parse(resources / qualifier / "strings.xml").getroot()
        entries = list(strings)
        keys = [entry.attrib["name"] for entry in entries]
        assert len(keys) == len(set(keys)), f"Duplicate keys: {qualifier}"
        assert all("".join(entry.itertext()).strip() for entry in entries), qualifier
        signatures = {entry.attrib["name"]: re.findall(r"%(?:\d+\$)?[a-zA-Z]", "".join(entry.itertext())) for entry in entries}
        if baseline is None:
            baseline = signatures
        else:
            assert signatures == baseline, f"Missing keys or incompatible placeholders: {qualifier}"
    for path in resources.rglob("*.xml"):
        ET.parse(path)
    manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
    assert not manifest.findall("uses-permission"), "Bootstrap must not request permissions"
    merger = "{http://schemas.android.com/tools}"
    uses_sdk = manifest.find("uses-sdk")
    assert uses_sdk is not None and uses_sdk.attrib == {merger + "overrideLibrary": "androidx.heifwriter"}, \
        "Only the audited AndroidX writer may override the dependency minSdk manifest check"
    assert "minSdk = 26" in (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8"), \
        "Application minSdk must remain 26"
    app = manifest.find("application")
    assert app is not None and app.attrib[ANDROID + "allowBackup"] == "false"
    assert app.attrib.get(ANDROID + "largeHeap", "false") == "false"
    with (ROOT / "gradle/libs.versions.toml").open("rb") as stream:
        catalog = tomllib.load(stream)
    assert all("+" not in value and "SNAPSHOT" not in value for value in catalog["versions"].values())
    print(f"PASS: {len(baseline)} resource keys in en/es/ca; XML, bootstrap permissions and exact catalog versions")

if __name__ == "__main__":
    check()
