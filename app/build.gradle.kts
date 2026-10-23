plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "org.cajapersonal.fixmyshot"
    compileSdk = 36
    buildToolsVersion = "35.0.0"
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
        debug { applicationIdSuffix = ".debug" }
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


// One checked-in icon serves store metadata, documentation and Android resources.
// Generated packaging copies stay under build/, never as a second committed PNG.
val prepareBrandingResources by tasks.registering(Sync::class) {
    from(rootProject.layout.projectDirectory.file("fastlane/metadata/android/en-US/images/icon.png"))
    into(layout.buildDirectory.dir("generated/branding/res/drawable-nodpi"))
    rename { "brand_icon.png" }
}
android.sourceSets.getByName("main").res.srcDir(layout.buildDirectory.dir("generated/branding/res"))
tasks.named("preBuild").configure { dependsOn(prepareBrandingResources) }
