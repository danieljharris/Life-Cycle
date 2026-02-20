// ============================================================
// GRADLE FOR IDE SUPPORT ONLY - DO NOT USE FOR BUILDS
// Actual builds use Bazel - run ./build.sh
// ============================================================

plugins {
    kotlin("jvm") version "2.3.0" apply false
    java
}

allprojects {
    repositories {
        mavenCentral()
        // Local flat directory for HytaleServer.jar (downloaded by setup script)
        flatDir {
            dirs(rootProject.file("hytale-downloader/Server"))
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
