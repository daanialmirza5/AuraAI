plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    api(project(":core-ai"))
    api(project(":core-intent"))
    api(project(":core-tools"))
    api(project(":core-memory"))
    api(project(":core-providers"))
    api(project(":core-planner"))
    api(project(":core-reasoning"))
    api(project(":core-events"))
    api(project(":core-capabilities"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
