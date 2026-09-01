pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AURA AI"
include(":app")
include(":core-ai")
include(":core-memory")
include(":core-intent")
include(":core-planner")
include(":core-actions")
include(":core-providers")
include(":core-tools")
include(":core-reasoning")
include(":core-events")
include(":core-capabilities")
include(":core-agents")
include(":core-orchestrator")
include(":plugin-api")
include(":core-plugin")
include(":plugin-runtime")
include(":plugin-loader")
include(":plugin-marketplace")
