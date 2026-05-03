import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "com.svd.svdagencies"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.svd.svdagencies"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Define a release config that points to the debug keystore as a fallback
        val releaseConfig = create("release") {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        // Redirect the problematic 'externalOverride' config if it's being injected
        maybeCreate("externalOverride").apply {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            // Explicitly use the release config defined above (which uses debug credentials)
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    packaging {
        resources {
            excludes.add("assets/Payments-Loader.json")
        }
    }
}

dependencies {
    // Standard AndroidX & UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.swiperefresh)
    
    // Network & API
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.paytm.appinvokesdk:appinvokesdk:1.6.17") {
        exclude(group = "com.squareup.okhttp3", module = "okhttp")
    }
    
    // Architecture Components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.fragment.ktx)
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Image Loading
    implementation(libs.glide)
    implementation(libs.glide.okhttp)
    kapt(libs.glide.compiler)
    
    // Utilities
    implementation(libs.zxing)
    implementation(libs.circleimageview)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.gridlayout:gridlayout:1.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
