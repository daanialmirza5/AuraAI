plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

// core-ai is deliberately not declared here — this module works entirely in plugin-api's own
// PluginResult/PluginError vocabulary; core-plugin already re-exposes core-ai via `api` for
// anything downstream that needs AuraResult/AuraError directly.
dependencies {
    api(project(":plugin-api"))
    api(project(":core-plugin"))
    api(project(":core-events"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
