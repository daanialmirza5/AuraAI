plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.detekt) apply false
}

// Applied to every module (all 18 have Kotlin source) rather than opted into one at a time —
// same reasoning as the version catalog itself: one place to configure, not 18. detekt uses a
// per-module baseline (config/detekt/baseline.xml, generated once via `detektBaseline`) to
// grandfather pre-existing findings across ~8 milestones of code that predates this milestone,
// while still failing CI on any *new* finding — see docs/CI_CD.md §2.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.3.1")
    }

    extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        baseline = file("$rootDir/config/detekt/baseline-${project.name}.xml")
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        // Detekt's bundled compiler doesn't recognize every JDK version as a --jvm-target value
        // (e.g. JDK 23, which this environment's Gradle daemon runs on) — pin explicitly to what
        // the project actually compiles for (JavaVersion.VERSION_17 everywhere) rather than
        // letting detekt infer it from the host JDK.
        jvmTarget = "17"
        reports {
            xml.required.set(true)
            html.required.set(true)
            sarif.required.set(false)
            txt.required.set(false)
        }
    }
    tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
        jvmTarget = "17"
    }
}
