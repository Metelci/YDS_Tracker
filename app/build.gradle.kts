plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    }

android {
    namespace = "com.mtlc.studyplan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mtlc.studyplan"
        minSdk = 30
        targetSdk = 35
<<<<<<< Updated upstream
        versionCode = 21
        versionName = "1.5.2" //major improvements and localization fixes
=======
        versionCode = 23
        versionName = "1.5.4"
>>>>>>> Stashed changes

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        isCoreLibraryDesugaringEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    kotlin {
        jvmToolchain(11)
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.material.icons.extended)

    // Security dependencies
    implementation(libs.security.crypto)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.biometric)
    implementation(libs.kotlinx.serialization)

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mockito)
    testImplementation("androidx.test:core:1.5.0")
    testImplementation(libs.androidx.junit)
    testImplementation(libs.coroutines.test)
    testImplementation("org.robolectric:robolectric:4.10.3")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}