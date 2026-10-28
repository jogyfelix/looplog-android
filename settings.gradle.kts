pluginManagement {
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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HabitApp"
include(":app",":feature:habit",":feature:settings",
    ":core:design-system")

include(":core:storage")
include(":core:model")
include(":core:data")
include(":core:domain")
include(":core:ui")
include(":core:utils")
include(":core:ai-engine")
include(":model_pack")
include(":core:analytics")
