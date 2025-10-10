# Dependency Management

## Overview
This document provides an overview of all external dependencies used in the StudyPlan project and their purposes.

## Core Dependencies

### AndroidX Libraries
- `androidx.core:core-ktx` - Kotlin extensions for Android core APIs
- `androidx.lifecycle:*` - Lifecycle-aware components (runtime, viewmodel, process)
- `androidx.activity:activity-compose` - Compose integration for Activities
- `androidx.navigation:navigation-compose` - Navigation for Compose UI
- `androidx.room:*` - Database (runtime, compiler, ktx)
- `androidx.datastore:datastore-preferences` - Preferences data store
- `androidx.work:work-runtime-ktx` - Work manager for background tasks

### Compose Libraries
- `androidx.compose.*` - Various Compose libraries (BOM, UI, Foundation, Material3)
- `androidx.lifecycle:lifecycle-viewmodel-compose` - ViewModel integration for Compose

### Dependency Injection
- `org.koin:koin-*` - Koin dependency injection framework (core, android, compose)
- `com.google.dagger:hilt-android` - Hilt (though migrating to Koin)
- `androidx.hilt:*` - Hilt navigation and work integration

### Network & Data
- `com.squareup.okhttp3:*` - HTTP client
- `com.google.code.gson:gson` - JSON parsing
- `com.squareup.retrofit2:*` - REST API client

### Image Loading
- `io.coil-kt:coil-compose` - Image loading for Compose

### Firebase
- `com.google.firebase:firebase-bom` and `firebase-messaging` - Firebase services

### Testing
- `junit:junit`, `org.mockito:*`, `androidx.test:*` - Testing frameworks
- `org.robolectric:robolectric` - Unit testing framework

### Other
- `org.jetbrains.kotlinx:kotlinx-serialization-json` - JSON serialization
- `androidx.biometric:biometric` - Biometric authentication
- `androidx.security:security-crypto` - Security and cryptography

## KSP (Kotlin Symbol Processing)
- `androidx.room:room-compiler` - Room annotation processor
- `com.google.dagger:hilt-compiler` - Hilt annotation processor
- KSP plugin for annotation processing

## Gradle Plugins
Dependencies managed in plugins block:
- Android application plugin
- Kotlin Android plugin
- Kotlin Compose plugin
- Kotlin serialization plugin
- Detekt plugin for static analysis
- KSP plugin for annotation processing

## Dependency Version Management
- Versions are managed through libs.versions.toml file (referenced via libs alias)
- Gradle Version Catalog for consistent dependency management
- Use `./gradlew dependencies` to generate current dependency tree