import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Release signing is optional and never committed: keystore.properties (already .gitignore'd,
// alongside *.jks/*.keystore) supplies storeFile/storePassword/keyAlias/keyPassword when a real
// release keystore exists. Its absence (true today, and true in CI — see docs/CI_CD.md §3) means
// `release` simply has no signingConfig assigned, exactly as before this milestone: assembleRelease
// still succeeds and still produces a real, R8-minified build, just an unsigned one that can't be
// installed or uploaded until the app's publisher generates and owns their own keystore — a secret
// this project cannot generate on their behalf (see docs/TODO_V1.md Section 2.10).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use { load(it) }
        }
    }
val hasReleaseSigningConfig = keystorePropertiesFile.exists()

android {
    namespace = "com.aura.ai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aura.ai"
        minSdk = 26
        targetSdk = 35
        versionCode = 12
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)
    implementation(libs.splashscreen)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.biometric)
    implementation(libs.fragment.ktx)
    implementation(libs.collections.immutable)
    implementation(libs.security.crypto)
    // Needed directly (not just transitively via :core-providers) because AiProvidersModule,
    // which lives here, constructs the shared OkHttpClient the four HTTP-backed providers use.
    implementation(libs.okhttp)

    implementation(project(":core-ai"))
    implementation(project(":core-memory"))
    implementation(project(":core-intent"))
    implementation(project(":core-planner"))
    implementation(project(":core-actions"))
    implementation(project(":core-providers"))
    implementation(project(":core-tools"))
    implementation(project(":core-reasoning"))
    implementation(project(":core-events"))
    implementation(project(":core-capabilities"))
    implementation(project(":core-agents"))
    implementation(project(":core-orchestrator"))
    implementation(project(":plugin-api"))
    implementation(project(":core-plugin"))
    implementation(project(":plugin-runtime"))
    implementation(project(":plugin-loader"))
    implementation(project(":plugin-marketplace"))

    testImplementation(libs.junit4)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
