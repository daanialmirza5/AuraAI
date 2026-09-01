plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core-ai"))
    api(project(":core-intent"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
