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
    }
}

rootProject.name = "LocalBites"

include(":app")
include(":core:ui")
include(":core:network")
include(":core:database")
include(":core:common")
include(":feature:auth")
include(":feature:customer")
include(":feature:restaurant")
include(":feature:delivery")
