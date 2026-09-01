plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

// core-capabilities and core-events are deliberately not declared here — plugin capabilities are
// their own independent, string-keyed registry (see registrar/PluginCapabilityRegistry.kt for
// why), and bridging plugin lifecycle transitions into core-events' AuraEvent is plugin-runtime's
// job, not core-plugin's.
dependencies {
    api(project(":plugin-api"))
    api(project(":core-ai"))
    api(project(":core-tools"))
    api(project(":core-intent"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
