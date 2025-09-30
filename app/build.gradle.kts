plugins {
    // Android application plugins
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    
    // Compose and Kotlin plugins
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    
    // Kotlin Symbol Processing (KSP) for code generation
    id("com.google.devtools.ksp") version "2.0.21-1.0.25"
}

android {
    namespace = "com.mtlc.studyplan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mtlc.studyplan"
        minSdk = 30
        targetSdk = 35
        versionCode = 65
        versionName = "2.9.28"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildTypes {
        debug {
            // Disable APK splits for debug builds to prevent dex loading issues
            enableAndroidTestCoverage = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use debug signing for local builds, release signing will be configured in CI/CD
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // Disable APK splits to prevent dex loading issues
    splits {
        abi {
            isEnable = false
        }
    }

    // Disable bundle splits for debug builds
    bundle {
        language {
            enableSplit = false
        }
        density {
            enableSplit = false
        }
        abi {
            enableSplit = false
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
        viewBinding = true
    }
    
    
    // Enable R8 for release builds to reduce APK size
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        // Use a baseline so existing issues don't fail CI; new issues will
        // still be reported and can be fixed incrementally.
        baseline = file("lint-baseline.xml")
        disable.add("SuspiciousModifierThen")
        disable.add("NullSafeMutableLiveData")
        disable.add("FrequentlyChangingValue")
        disable.add("RememberInComposition")
        disable.add("AutoboxingStateCreation")
    }

    // Fix Gradle 9.0 compatibility warnings
    configurations.all {
        if (name.contains("RuntimeClasspathCopy")) {
            isCanBeConsumed = false
        }
        if (isCanBeResolved && isCanBeConsumed) {
            if (name.endsWith("ClasspathCopy") || name.contains("RuntimeClasspath")) {
                isCanBeConsumed = false
            }
        }
    }
}


dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs.v215)

    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // XML View System Dependencies (for legacy XML layouts)
    //noinspection UseTomlInstead
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    // Security dependencies
    implementation(libs.security.crypto)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.biometric)
    implementation(libs.kotlinx.serialization)

    // JSON processing
    implementation(libs.gson)

    // Image loading and processing for avatar upload
    implementation(libs.coil.compose)
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Room (local database for scalable histories)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    //noinspection UseTomlInstead
    ksp(libs.androidx.room.compiler)

    // Dependency Injection with Koin
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // JavaPoet will be included automatically by Hilt

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mockito)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.arch.core.testing)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.accessibility.test.framework)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // javax.inject annotations retained for existing @Inject/@Singleton usage
    implementation(libs.javax.inject)
}

ksp {
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

// Work around IDE/NetBeans eagerly resolving ASM transform outputs for unit tests.
// Completely disable the problematic unit test transform tasks to prevent
// "Querying the mapped value ... before task has completed" errors during sync/tests.
// Does not affect app assemble or androidTest tasks.
afterEvaluate {
    tasks.findByName("transformDebugUnitTestClassesWithAsm")?.enabled = false
    tasks.findByName("transformReleaseUnitTestClassesWithAsm")?.enabled = false
}

















