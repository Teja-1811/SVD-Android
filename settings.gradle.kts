pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://artifactory.paytm.in/libs-release-local") }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "SVDAgencies"
include(":app")
