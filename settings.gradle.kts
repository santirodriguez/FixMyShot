pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
check(providers.gradleProperty("android.enableJetifier").orNull in listOf(null, "false")) {
    "FixMyShot does not permit Jetifier; use AndroidX dependencies directly."
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "FixMyShot"
include(":app", ":core", ":codec-native")

// Never configure or build the separate physical AVIF probe during ordinary FixMyShot builds.
if (providers.gradleProperty("fixmyshot.avifPhysicalProbe").orNull == "true") {
    include(":avif-physical")
}
