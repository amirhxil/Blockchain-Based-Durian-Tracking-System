// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://repo.maven.apache.org/maven2") } // Add this for fallback
    }
    dependencies {
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.46") // Add this line
        classpath("com.android.tools.build:gradle:8.1.4")// Ensure correct Gradle version
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0") // Ensure your Kotlin plugin version is compatible
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.5.3")

    }
}


