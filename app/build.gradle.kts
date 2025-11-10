plugins {
    // Android application plugins
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Compose and Kotlin plugins
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)

    // Static analysis and code quality
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    jacoco // Test coverage reporting

    // Kotlin Symbol Processing (KSP) for code generation
    id("com.google.devtools.ksp") version libs.versions.ksp.get()
    
    // Dependency vulnerability scanning
    id("org.owasp.dependencycheck") version "12.1.8"
}

android {
    namespace = "com.mtlc.studyplan"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mtlc.studyplan"
        minSdk = 30
        targetSdk = 35
        versionCode = 95
        versionName = "2.9.66"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    signingConfigs {
        create("release") {
            // Production signing configuration with fallback support
            // Priority: Environment Variables > Gradle Properties > Local Properties > Debug Keystore
            val keystorePath = System.getenv("KEYSTORE_PATH")
                ?: project.properties["keystore.path"]?.toString()
                ?: project.properties["signing.store.file"]?.toString()
                ?: (System.getProperty("user.home") + "/.android/debug.keystore")
            val keystorePassword = System.getenv("KEYSTORE_PASSWORD")
                ?: project.properties["keystore.password"]?.toString()
                ?: project.properties["signing.store.password"]?.toString()
                ?: "android"
            val keyAlias = System.getenv("KEY_ALIAS")
                ?: project.properties["key.alias"]?.toString()
                ?: project.properties["signing.key.alias"]?.toString()
                ?: "androiddebugkey"
            val keyPassword = System.getenv("KEY_PASSWORD")
                ?: project.properties["key.password"]?.toString()
                ?: project.properties["signing.key.password"]?.toString()
                ?: "android"
            storeFile = file(keystorePath)
            storePassword = keystorePassword
            this.keyAlias = keyAlias
            this.keyPassword = keyPassword
        }
    }
    buildTypes {
        debug {
            // Disable APK splits for debug builds to prevent dex loading issues
            enableAndroidTestCoverage = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true  // Enable resource shrinking to remove unused resources
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use debug signing for local builds, release signing will be configured in CI/CD
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // Disable APK splits to prevent dex loading issues
    splits {
        abi {
            isEnable = false
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
        buildConfig = true
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

    // Ensure unit tests do not try to compile data binding generated sources
    @Suppress("UnstableApiUsage")
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    // Exclude data binding generated sources from unit test compilation via test task configuration
    // (sourceSets.exclude is not supported this way in AGP 8 Kotlin DSL)

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
    implementation(libs.androidx.lifecycle.process)
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

    // HTTP client for API calls
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)

    // Image loading and processing for avatar upload
    implementation(libs.coil.compose)
    implementation(libs.exifinterface)

    // Firebase Cloud Messaging for push notifications
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    
    // Timber Logging Framework
    implementation(libs.timber)

    // Room (local database for scalable histories)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    //noinspection UseTomlInstead
    ksp(libs.androidx.room.compiler)
    // Room testing utilities (MigrationTestHelper, InMemory)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.room.testing)

    // Dependency Injection with Koin
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // Baseline Profile for app startup optimization
    implementation(libs.profileinstaller)

    // Testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mockito)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.mockk)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    // Ensure ProcessLifecycleOwner is available under Robolectric
    testImplementation(libs.androidx.lifecycle.process)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.koin.test)
    testImplementation(libs.koin.test.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.arch.core.testing)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.accessibility.test.framework)
    androidTestImplementation(libs.mockito)
    androidTestImplementation(libs.mockito.kotlin)
    // WorkManager testing utilities (instrumentation)
    //noinspection UseTomlInstead
    androidTestImplementation("androidx.work:work-testing:2.11.0")
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // javax.inject annotations retained for existing @Inject/@Singleton usage
    implementation(libs.javax.inject)
}

ksp {
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$projectDir/detekt.yml")
    baseline = file("$projectDir/detekt-baseline.xml")
}

// Work around IDE/NetBeans eagerly resolving ASM transform outputs for unit tests.
// Completely disable the problematic unit test transform tasks to prevent
// "Querying the mapped value ... before task has completed" errors during sync/tests.
// Does not affect app assemble or androidTest tasks.
afterEvaluate {
    tasks.findByName("transformDebugUnitTestClassesWithAsm")?.enabled = false
    tasks.findByName("transformReleaseUnitTestClassesWithAsm")?.enabled = false
}

// JaCoCo Test Coverage Configuration
jacoco {
    toolVersion = "0.8.12"
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/*\$Lambda$*.*",
        "**/*\$inlined$*.*",
        "**/*Hilt*.*",
        "**/*_Factory.*",
        "**/*_MembersInjector.*",
        "**/databinding/**",
        "**/generated/**"
    )

    val debugTree = fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes").get().asFile) {
        exclude(fileFilter)
    }
    val kotlinDebugTree = fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug").get().asFile) {
        exclude(fileFilter)
    }

    classDirectories.setFrom(files(debugTree, kotlinDebugTree))
    sourceDirectories.setFrom(files("${project.projectDir}/src/main/java", "${project.projectDir}/src/main/kotlin"))
    executionData.setFrom(fileTree(layout.buildDirectory.get().asFile) {
        include("**/*.exec", "**/*.ec")
    })
}

tasks.register("jacocoTestCoverageVerification", JacocoCoverageVerification::class) {
    dependsOn("jacocoTestReport")

    violationRules {
        rule {
            limit {
                // Keep local threshold in sync with CI expectations.
                minimum = "0.20".toBigDecimal()
            }
        }
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )

    classDirectories.setFrom(
        files(
            fileTree(layout.buildDirectory.dir("intermediates/javac/debug/classes").get().asFile) {
                exclude(fileFilter)
            },
            fileTree(layout.buildDirectory.dir("tmp/kotlin-classes/debug").get().asFile) {
                exclude(fileFilter)
            }
        )
    )
}


// OWASP Dependency-Check Configuration
dependencyCheck {
    format = "HTML"
}

tasks.matching { it.name.startsWith("dependencyCheck") }.configureEach {
    onlyIf { project.hasProperty("dependencyCheckEnabled") }
}
