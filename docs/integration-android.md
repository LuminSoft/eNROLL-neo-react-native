# Android Integration Guide

## Prerequisites

- React Native >= 0.71.0
- Android Studio (latest stable)
- minSdkVersion 24 or higher
- Kotlin 2.0+ (the eNROLL Neo SDK is compiled with Kotlin 2.1.0 metadata)

## Step 1: Install the Plugin

```bash
npm install enroll-neo-react-native
```

## Step 2: Add Maven Repositories

The eNROLL Neo SDK is hosted on JitPack. Add this repository.

**Option A — `settings.gradle` (recommended for RN 0.76+):**

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**Option B — project-level `build.gradle`:**

```groovy
buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

## Step 3: Verify minSdkVersion & Kotlin Version

In `android/build.gradle`:

```groovy
buildscript {
    ext {
        minSdkVersion = 24
        kotlinVersion = "2.0.21"
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    }
}
```

> **Important:** The eNROLL Neo SDK was compiled with Kotlin 2.1.0. You need at least Kotlin 2.0.21 to read its metadata. If you use an older Kotlin (e.g. 1.9.x), you will see `Unresolved reference` compilation errors.

## Step 4: ProGuard / R8 Rules (Release Builds)

If you enable minification, add to `android/app/proguard-rules.pro`:

```proguard
-keep class com.luminsoft.enroll_sdk.** { *; }
-dontwarn com.luminsoft.enroll_sdk.**
```

## Step 5: Build

```bash
cd android && ./gradlew :app:assembleDebug
```
