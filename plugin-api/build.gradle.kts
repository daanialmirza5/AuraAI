plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

// Deliberately no project() dependencies at all — not even core-ai. This is the plugin SDK
// surface itself: a plugin author's compiled code only ever sees what's declared in this module,
// so "zero dependencies" is what makes "plugins cannot directly access internal modules" true by
// construction, not just by convention. See docs/PLUGIN_SECURITY.md.
dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
