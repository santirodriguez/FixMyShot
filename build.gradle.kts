plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
allprojects {
    dependencyLocking { lockAllConfigurations() }
    // Dependency locking is the resolution mode; Gradle rejects combining it
    // with failOnDynamicVersions/failOnChangingVersions. Direct pins are checked
    // by check-bootstrap.py, and resolved locks require review before G1 closes.
}
