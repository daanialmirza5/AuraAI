plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // core-tools, core-memory, core-planner, and core-reasoning are deliberately not declared
    // here — core-orchestrator never imports a type from them directly (it only ever sees them
    // through the `Agent` abstraction), and core-agents already re-exposes them via `api` for
    // anything downstream that does need them.
    api(project(":core-ai"))
    api(project(":core-intent"))
    api(project(":core-providers"))
    api(project(":core-events"))
    api(project(":core-capabilities"))
    api(project(":core-agents"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
