#!/usr/bin/env python3
"""Check the reviewed Gradle 8.13 launcher before executing any Java/download code."""
import hashlib
from pathlib import Path
import sys

# JAR/distribution anchors: https://gradle.org/release-checksums/ (8.13).
# Launcher scripts: source-bound, verified-distribution generated artifact12489096076.
FILES = {
    'gradle/wrapper/gradle-wrapper.jar': '81a82aaea5abcc8ff68b3dfcb58b3c3c429378efd98e7433460610fecd7ae45f',
    'gradlew': '734b3879d3501dce471cf0522d3bcbafe76873d9fc5129345b67fb43bd15e933',
    'gradlew.bat': '57931b17dd228e5c24dac90e815d0bf82477e831a4618dfab4136f5446b42a9f',
}
PROPERTIES = {
    'distributionBase': 'GRADLE_USER_HOME',
    'distributionPath': 'wrapper/dists',
    'distributionSha256Sum': '20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78',
    'distributionUrl': r'https\://services.gradle.org/distributions/gradle-8.13-bin.zip',
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
    print('PASS: committed Gradle 8.13 wrapper and distribution pins; no network or Java execution')
