// ============================================================
// GRADLE FOR IDE SUPPORT ONLY - DO NOT USE FOR BUILDS
// Actual builds use Bazel - run ./build.sh
// ============================================================

plugins {
    kotlin("jvm") version "2.1.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    main {
        kotlin.srcDirs("src/main/kotlin")
        java.srcDirs("src/main/java")
        resources.srcDirs("src/main/resources")
    }
}

dependencies {
    // Kotlin stdlib (version managed by Kotlin plugin)
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.16")
    
    // JSON
    implementation("com.google.code.gson:gson:2.13.1")
    
    // Hytale Server API - load from local JAR if available
    compileOnly(files(rootProject.file(".local-assets/HytaleServer.jar")))
}
