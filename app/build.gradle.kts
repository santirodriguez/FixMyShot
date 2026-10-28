plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "org.cajapersonal.fixmyshot"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "org.cajapersonal.fixmyshot"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-alpha.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }
    buildTypes {
        debug { applicationIdSuffix = ".automatic" }
        release { isMinifyEnabled = false }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { jniLibs { useLegacyPackaging = false } }
    lint { abortOnError = true; warningsAsErrors = false }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":core"))
    implementation(project(":codec-native"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.exifinterface)
    implementation(libs.appcompat)
    implementation(libs.datastore.preferences)
    androidTestImplementation(libs.android.test.runner)
    androidTestImplementation(libs.android.test.junit)
    // The target app already packages HeifWriter via :codec-native; tests only need its types.
    androidTestCompileOnly(libs.heifwriter)
}


// Typed task outputs keep generated resources wired to each Android variant.
abstract class PrepareBrandingTask : DefaultTask() {
    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.NONE)
    abstract val sourceIcon: org.gradle.api.file.RegularFileProperty
    @get:org.gradle.api.tasks.InputFile
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.NONE)
    abstract val sourceHeader: org.gradle.api.file.RegularFileProperty
    @get:org.gradle.api.tasks.OutputDirectory
    abstract val outputDirectory: org.gradle.api.file.DirectoryProperty

    @org.gradle.api.tasks.TaskAction
    fun generate() {
        val target = outputDirectory.file("drawable-nodpi/brand_icon.png").get().asFile
        target.parentFile.mkdirs()
        sourceIcon.get().asFile.copyTo(target, overwrite = true)
        sourceHeader.get().asFile.copyTo(outputDirectory.file("drawable-nodpi/brand_header.png").get().asFile,
            overwrite = true)
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
            sourceHeader.set(rootProject.layout.projectDirectory.file("assets/branding/header.png"))
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
