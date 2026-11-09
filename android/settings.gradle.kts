pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolution {
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
