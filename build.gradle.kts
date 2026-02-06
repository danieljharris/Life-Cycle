// ============================================================
// GRADLE FOR IDE SUPPORT ONLY - DO NOT USE FOR BUILDS
// Actual builds use Bazel - run ./build.sh
// ============================================================

plugins {
    kotlin("jvm") version "2.1.0" apply false
    java
}

allprojects {
    repositories {
        mavenCentral()
        // Hytale server JAR repository (if available)
        maven {
            url = uri("https://artifacts.yakovliam.com/")
            isAllowInsecureProtocol = true
        }
        // Local flat directory for HytaleServer.jar
        flatDir {
            dirs(rootProject.file(".local-assets"))
        }
    }
}

subprojects {
    apply(plugin = "java")
    
    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
