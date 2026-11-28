plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.ztemeshmanager"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.example.ztemeshmanager"
        minSdk = 23
        targetSdk = 35
        versionCode = 64
        versionName = "0.7.3"
    }

    // Keep Java and Kotlin bytecode targets aligned for Newly's release build.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
