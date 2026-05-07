pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "kdb"

include(":kdb-core")
include(":kdb-driver")
include(":kdb-schema")
include(":kdb-query")
include(":kdb-paging")
include(":kdb-paging3")
include(":kdb-client")
