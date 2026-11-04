#!/usr/bin/env python3
"""Prepare a source-pinned experimental checkout; never updates repository refs."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

DIST_SHA = '2ab2958f2a1e51120c326cad6f385153bb11ee93b3c216c5fccebfdfbb7ec6cb'
WRAPPER_SHA = '55243ef57851f12b070ad14f7f5bb8302daceeebc5bce5ece5fa6edb23e1145c'
SOURCE = 'ad075068564dc8a7d8d0eca35fc1f69ff3d16464'
TREE = 'b5ecadc6a756e0c07a67b7f0b38ae354744d3b8b'
APP_GENERATORS_OLD = '''// One checked-in icon serves store metadata, documentation and Android resources.
// Generated packaging copies stay under build/, never as a second committed PNG.
val prepareBrandingResources by tasks.registering(Sync::class) {
    from(rootProject.layout.projectDirectory.file("fastlane/metadata/android/en-US/images/icon.png"))
    into(layout.buildDirectory.dir("generated/branding/res/drawable-nodpi"))
    rename { "brand_icon.png" }
}
android.sourceSets.getByName("main").res.srcDir(layout.buildDirectory.dir("generated/branding/res"))
tasks.named("preBuild").configure { dependsOn(prepareBrandingResources) }

// Package canonical legal texts without maintaining separate Android copies.
val prepareLegalAssets by tasks.registering(Sync::class) {
    from(rootProject.layout.projectDirectory.file("LICENSE")) { into("project") }
    from(rootProject.layout.projectDirectory.file("THIRD_PARTY_NOTICES"))
    from(rootProject.layout.projectDirectory.dir("LICENSES")) { into("licenses") }
    into(layout.buildDirectory.dir("generated/legal/assets/legal"))
}
android.sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/legal/assets"))
tasks.named("preBuild").configure { dependsOn(prepareLegalAssets) }
'''
APP_GENERATORS_NEW = '''// Typed task outputs keep generated resources wired to each Android variant.
abstract class PrepareBrandingTask : DefaultTask() {
    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.NONE)
    abstract val sourceIcon: org.gradle.api.file.RegularFileProperty
    @get:org.gradle.api.tasks.OutputDirectory
    abstract val outputDirectory: org.gradle.api.file.DirectoryProperty

    @org.gradle.api.tasks.TaskAction
    fun generate() {
        val target = outputDirectory.file("drawable-nodpi/brand_icon.png").get().asFile
        target.parentFile.mkdirs()
        sourceIcon.get().asFile.copyTo(target, overwrite = true)
    }
}

abstract class PrepareLegalTask : DefaultTask() {
    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.NONE)
    abstract val projectLicense: org.gradle.api.file.RegularFileProperty
    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.NONE)
    abstract val thirdPartyNotices: org.gradle.api.file.RegularFileProperty
    @get:org.gradle.api.tasks.InputDirectory
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    abstract val licenseDirectory: org.gradle.api.file.DirectoryProperty
    @get:org.gradle.api.tasks.OutputDirectory
    abstract val outputDirectory: org.gradle.api.file.DirectoryProperty

    @org.gradle.api.tasks.TaskAction
    fun generate() {
        val legal = outputDirectory.dir("legal").get().asFile
        if (legal.exists()) check(legal.deleteRecursively()) { "Cannot clear generated legal assets" }
        check(legal.resolve("project").mkdirs()) { "Cannot create generated legal directory" }
        projectLicense.get().asFile.copyTo(legal.resolve("project/LICENSE"), overwrite = true)
        thirdPartyNotices.get().asFile.copyTo(legal.resolve("THIRD_PARTY_NOTICES"), overwrite = true)
        check(licenseDirectory.get().asFile.copyRecursively(legal.resolve("licenses"), overwrite = true))
    }
}

androidComponents {
    onVariants(selector().all()) { variant ->
        val suffix = variant.name.replaceFirstChar { it.uppercase() }
        val branding = tasks.register<PrepareBrandingTask>("prepare${suffix}BrandingResources") {
            sourceIcon.set(rootProject.layout.projectDirectory.file("fastlane/metadata/android/en-US/images/icon.png"))
            outputDirectory.set(layout.buildDirectory.dir("generated/branding/${variant.name}/res"))
        }
        val legal = tasks.register<PrepareLegalTask>("prepare${suffix}LegalAssets") {
            projectLicense.set(rootProject.layout.projectDirectory.file("LICENSE"))
            thirdPartyNotices.set(rootProject.layout.projectDirectory.file("THIRD_PARTY_NOTICES"))
            licenseDirectory.set(rootProject.layout.projectDirectory.dir("LICENSES"))
            outputDirectory.set(layout.buildDirectory.dir("generated/legal/${variant.name}/assets"))
        }
        variant.sources.res?.addGeneratedSourceDirectory(branding, PrepareBrandingTask::outputDirectory)
        variant.sources.assets?.addGeneratedSourceDirectory(legal, PrepareLegalTask::outputDirectory)
    }
}
'''
RECIPE_REVISION = 2

EDITS = {
    'gradle/libs.versions.toml': [('agp = "8.13.2"', 'agp = "9.2.1"'),
        ('kotlin = "2.2.21"', 'kotlin = "2.4.21"'),
        ('kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }\n', '')],
    'build.gradle.kts': [('    alias(libs.plugins.kotlin.android) apply false\n', '')],
    'app/build.gradle.kts': [('    alias(libs.plugins.kotlin.android)\n', ''),
                           ('buildToolsVersion = "35.0.0"', 'buildToolsVersion = "36.0.0"'),
                           (APP_GENERATORS_OLD, APP_GENERATORS_NEW)],
    'codec-native/build.gradle.kts': [('    alias(libs.plugins.kotlin.android)\n', ''),
                           ('buildToolsVersion = "35.0.0"', 'buildToolsVersion = "36.0.0"')],
    'scripts/setup-android.sh': [("'build-tools;35.0.0'", "'build-tools;36.0.0'")],
    'gradle/wrapper/gradle-wrapper.properties': [
        ('gradle-8.13-bin.zip', 'gradle-9.4.1-bin.zip'),
        ('20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78', DIST_SHA)],
}
LOCKS = ('app/gradle.lockfile', 'core/gradle.lockfile', 'codec-native/gradle.lockfile')


def git(root, *args):
    return subprocess.check_output(['git', '-C', str(root), *args], text=True).strip()


def prepare(root, request):
    if (request.get('schema') != 1 or request.get('source_commit') != SOURCE or
            request.get('source_tree') != TREE or request.get('scope') != 'EXPERIMENTAL_DERIVED_CHECKOUT' or
            request.get('recipe_revision') != RECIPE_REVISION or
            set(request.get('inputs', {})) != set(EDITS) | set(LOCKS)):
        raise ValueError('Unapproved migration source or input set')
    if git(root, 'rev-parse', 'HEAD') != SOURCE or git(root, 'rev-parse', 'HEAD^{tree}') != TREE:
        raise ValueError('Checkout differs from the inspected source')
    if git(root, 'status', '--porcelain', '--untracked-files=all'):
        raise ValueError('Migration requires a pristine checkout')
    # Validate every input and replacement before any mutation.
    outputs = {}
    for name, expected in request['inputs'].items():
        path = root / name
        if path.is_symlink() or not path.is_file() or path.stat().st_size > 262144:
            raise ValueError('Missing, linked or oversized migration input')
        data = path.read_bytes()
        if hashlib.sha256(data).hexdigest() != expected:
            raise ValueError('Migration input differs: ' + name)
        if name in EDITS:
            text = data.decode('utf-8')
            for old, new in EDITS[name]:
                if text.count(old) != 1:
                    raise ValueError('Unexpected migration edit anchor: ' + name)
                text = text.replace(old, new)
            outputs[name] = text
    for name, text in outputs.items():
        (root / name).write_text(text)
    # Only the three version locks in this disposable, pinned checkout are reset.
    # Resolution regenerates them; they are not promoted to source automatically.
    for name in LOCKS:
        (root / name).unlink()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('request', type=Path)
    parser.add_argument('workspace', type=Path)
    parser.add_argument('--verify-generated-wrapper', action='store_true')
    args = parser.parse_args()
    if args.verify_generated_wrapper:
        if hashlib.sha256((args.workspace / 'gradle/wrapper/gradle-wrapper.jar').read_bytes()).hexdigest() != WRAPPER_SHA:
            raise SystemExit('Generated wrapper differs from the official Gradle9.4.1 anchor')
        print('PASS: generated wrapper JAR matches independent official checksum')
    else:
        if args.request.stat().st_size > 8192:
            raise SystemExit('Oversized migration request')
        prepare(args.workspace, json.loads(args.request.read_text()))
        print('Prepared EXPERIMENTAL_DERIVED_CHECKOUT; not a source-frozen build or security approval')
