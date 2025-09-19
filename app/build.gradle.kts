plugins {
    // Android application plugins
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    
    // Compose and Kotlin plugins
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    
    // Kotlin Symbol Processing (KSP) for code generation
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"

    // Dependency Injection with Hilt
    id("com.google.dagger.hilt.android") version "2.48"
}

android {
    namespace = "com.mtlc.studyplan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mtlc.studyplan"
        minSdk = 30
        targetSdk = 35
        versionCode = 44
        versionName = "2.6.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Fix: Use proper signing config reference instead of debug config
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    kotlin {
        jvmToolchain(17)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.7.5"
    }
    
    // Enable R8 for release builds to reduce APK size
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
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
    implementation("androidx.compose.animation:animation-core")
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material")
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.material.icons.extended)
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // XML View System Dependencies (for legacy XML layouts)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    // Security dependencies
    implementation(libs.security.crypto)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.biometric)
    implementation(libs.kotlinx.serialization)

    // JSON processing
    implementation("com.google.code.gson:gson:2.10.1")

    // Room (local database for scalable histories)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Dependency Injection with Hilt
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mockito)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.accessibilityTestFramework)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

ksp {
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}













