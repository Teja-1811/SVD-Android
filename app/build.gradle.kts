import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.google.services)
}

configure<ApplicationExtension> {

    namespace = "com.svd.svdagencies"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.svd.svdagencies"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "1.0.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        maybeCreate("externalOverride").apply {
            storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    // Layouts are grouped by portal; shared values, images and menus remain in res.
    sourceSets.getByName("main").res.srcDirs(
        "src/main/res-admin/bills",
        "src/main/res-admin/companies",
        "src/main/res-admin/customers",
        "src/main/res-admin/dashboard",
        "src/main/res-admin/delivery-data",
        "src/main/res-admin/enquiries",
        "src/main/res-admin/finance",
        "src/main/res-admin/history",
        "src/main/res-admin/monthly-statements",
        "src/main/res-admin/offers-subscriptions",
        "src/main/res-admin/orders",
        "src/main/res-admin/products",
        "src/main/res-admin/reports",
        "src/main/res-admin/routes",
        "src/main/res-admin/settings",
        "src/main/res-admin/shared",
        "src/main/res-admin/stock", "src/main/res-delivery/bills",
        "src/main/res-delivery/dashboard",
        "src/main/res-delivery/navigation",
        "src/main/res-delivery/payments",
        "src/main/res-delivery/products",
        "src/main/res-delivery/reports",
        "src/main/res-delivery/stock",
        "src/main/res-delivery/subscriptions", "src/main/res-customer/bills",
        "src/main/res-customer/companies",
        "src/main/res-customer/dashboard",
        "src/main/res-customer/navigation",
        "src/main/res-customer/offers",
        "src/main/res-customer/orders",
        "src/main/res-customer/payments",
        "src/main/res-customer/products",
        "src/main/res-customer/statements",
        "src/main/res-customer/subscriptions",
        "src/main/res-customer/support",
        "src/main/res-user/bills",
        "src/main/res-user/companies",
        "src/main/res-user/dashboard",
        "src/main/res-user/navigation",
        "src/main/res-user/offers",
        "src/main/res-user/orders",
        "src/main/res-user/payments",
        "src/main/res-user/products",
        "src/main/res-user/stock",
        "src/main/res-user/subscriptions", "src/main/res-auth/entry",
        "src/main/res-auth/login",
        "src/main/res-auth/registration",
        "src/main/res-auth/welcome", "src/main/res-shared/common",
        "src/main/res-shared/companies",
        "src/main/res-shared/payments",
        "src/main/res-shared/settings",
        "src/main/res-shared/subscriptions",
        "src/main/res-shared/support"
    )

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {

        getByName("debug") {
            isMinifyEnabled = false
        }

        getByName("release") {
            isMinifyEnabled = true
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

    packaging {
        resources {
            excludes += "assets/Payments-Loader.json"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.swiperefresh)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.work)
    implementation(libs.fragment.ktx)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Glide
    implementation(libs.glide)
    implementation(libs.glide.okhttp)
    kapt(libs.glide.compiler)

    // Utilities
    implementation(libs.zxing)
    implementation(libs.circleimageview)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.gridlayout:gridlayout:1.1.0")
    implementation("com.airbnb.android:lottie:6.4.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}


// Native layout gallery (opt-in only). Normal application builds are unchanged.
if (providers.gradleProperty("portalSnapshots").orNull == "true") {
    configure<ApplicationExtension> {
        testOptions.unitTests.isIncludeAndroidResources = true
        sourceSets.getByName("test").java.srcDir("src/snapshots/java")
    }
    dependencies { add("testImplementation", "org.robolectric:robolectric:4.14.1") }
    tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
        maxHeapSize = "2048m"
        maxParallelForks = 1
        systemProperty("robolectric.dependency.repo.url", "https://repo.maven.apache.org/maven2")
        systemProperty("robolectric.dependency.repo.id", "central")
        systemProperty("maven.repo.local", rootProject.file(".codex-gradle-cache/robolectric-sdk").absolutePath)
        systemProperty("portal.snapshot.dir", providers.gradleProperty("portalSnapshotDir").get())
        systemProperty("portal.snapshot.manifest", providers.gradleProperty("portalSnapshotManifest").get())
        systemProperty("portal.snapshot.smoke", providers.gradleProperty("portalSnapshotSmoke").orNull ?: "false")
        testLogging { events("passed", "failed", "standardOut", "standardError") }
    }
}
