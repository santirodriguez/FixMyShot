#!/usr/bin/env python3
"""Check the reviewed Gradle 9.4.1 launcher before executing any Java/download code."""
import hashlib
from pathlib import Path
import sys

# JAR/distribution anchors: https://gradle.org/release-checksums/ (9.4.1).
# Launcher scripts: source-bound, verified-distribution generated experiment12583168008 atafea6e45.
FILES = {
    'gradle/wrapper/gradle-wrapper.jar': '55243ef57851f12b070ad14f7f5bb8302daceeebc5bce5ece5fa6edb23e1145c',
    'gradlew': 'aed171fb114f82e6eaea4970a245a200e0582a7dcc8ec0891ca41b6e4a62b754',
    'gradlew.bat': 'fedad02c18e266ec094995a5751b7fe1eb6e74f66bf75db64fae2e50eb22c234',
}
PROPERTIES = {
    'distributionBase': 'GRADLE_USER_HOME',
    'distributionPath': 'wrapper/dists',
    'distributionSha256Sum': '2ab2958f2a1e51120c326cad6f385153bb11ee93b3c216c5fccebfdfbb7ec6cb',
    'distributionUrl': r'https\://services.gradle.org/distributions/gradle-9.4.1-bin.zip',
    'networkTimeout': '10000',
    'validateDistributionUrl': 'true',
    'zipStoreBase': 'GRADLE_USER_HOME',
    'zipStorePath': 'wrapper/dists',
}


def check(root: Path) -> None:
    for name, expected in FILES.items():
        if hashlib.sha256((root / name).read_bytes()).hexdigest() != expected:
            raise ValueError(f'Unreviewed Gradle wrapper file: {name}')
    properties = {}
    for line in (root / 'gradle/wrapper/gradle-wrapper.properties').read_text().splitlines():
        if not line or line.startswith(('#', '!')):
            continue
        key, value = line.split('=', 1)
        if key in properties:
            raise ValueError('Duplicate wrapper property')
        properties[key] = value
    if properties != PROPERTIES:
        raise ValueError('Unreviewed Gradle wrapper distribution or options')


if __name__ == '__main__':
    try:
        check(Path(sys.argv[1]) if len(sys.argv) > 1 else Path(__file__).resolve().parents[1])
    except (OSError, ValueError) as error:
        sys.exit(f'FAIL: {error}')
    print('PASS: committed Gradle 9.4.1 wrapper and distribution pins; no network or Java execution')
