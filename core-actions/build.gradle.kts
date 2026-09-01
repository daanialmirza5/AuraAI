plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.aura.ai.core.actions"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    api(project(":core-ai"))
    api(project(":core-tools"))

    implementation(libs.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.javax.inject)

    // Only the @ApplicationContext qualifier annotation is needed here — this module does
    // not apply the Hilt plugin itself; the app module's Hilt graph does the actual codegen.
    implementation(libs.hilt.android)

    // DeferredActionWorker only needs the WorkManager runtime + the @HiltWorker/@AssistedInject
    // annotations — the Dagger codegen that wires HiltWorkerFactory together still happens in
    // :app's aggregating Hilt component, same as every other @Inject class in this module.
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)

    // OcrTool / BarcodeScanTool / ReceiptScanTool — on-device, no network round-trip, no per-call
    // cost. ImageUnderstandingTool (open-ended "what is this" questions, needing a real model)
    // lives in :app instead, alongside the other tools that need AIProviderManager/app-level
    // wiring — see docs/VISION_RUNTIME.md.
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.kotlinx.coroutines.play.services)

    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
