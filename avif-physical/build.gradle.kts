plugins {
    alias(libs.plugins.android.application)
}
// This entire module is excluded from ordinary settings.gradle.kts configurations.
check(providers.gradleProperty("fixmyshot.avifPhysicalProbe").orNull == "true") {
    "The physical AVIF app is opt-in only."
}
val nativeDirectory = providers.gradleProperty("fixmyshot.avifPhysicalLibsDir").orNull
    ?: error("Source-built physical JNI directory required")
val noticeDirectory = providers.gradleProperty("fixmyshot.avifPhysicalNoticesDir").orNull
    ?: error("Original native source notices are required")
check(file(nativeDirectory).isDirectory && file(noticeDirectory).isDirectory) {
    "Physical JNI libraries and notices must already exist."
}
android {
    namespace = "org.cajapersonal.fixmyshot.avifphysical"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "org.cajapersonal.fixmyshot.avifphysical"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "physical-probe.1"
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }
    sourceSets {
        getByName("main").jniLibs.srcDir(file(nativeDirectory))
        getByName("main").assets.srcDir(file(noticeDirectory))
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    packaging { jniLibs { useLegacyPackaging = false } }
    lint { abortOnError = true }
}
androidComponents {
    // This evidence-only app intentionally has NO release variant.
    beforeVariants(selector().withBuildType("release")) { it.enable = false }
}
