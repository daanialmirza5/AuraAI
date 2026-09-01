plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

// core-ai is deliberately not declared here — see plugin-runtime/build.gradle.kts for why.
dependencies {
    api(project(":plugin-api"))
    api(project(":core-plugin"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
