pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Galva"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":core:common")
include(":core:model")
include(":core:database")
include(":core:mediastore")
include(":core:data")
include(":core:vault")
include(":core:domain")
include(":core:designsystem")
include(":core:ui")

include(":feature:gallery")
include(":feature:albums")
include(":feature:search")
include(":feature:viewer")
include(":feature:secrets")
include(":feature:settings")
