plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("kotlin-parcelize")
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

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Fixed: Hyphens in Version Catalog are accessed with dots in Kotlin DSL
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.swiperefresh)
    
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.fragment.ktx)
    
    implementation(libs.glide)
    kapt(libs.glide.compiler)
    
    implementation(libs.zxing)
    implementation(libs.circleimageview)
    
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.gridlayout:gridlayout:1.1.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
