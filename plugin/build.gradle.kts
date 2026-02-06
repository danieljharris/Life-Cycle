// ============================================================
// GRADLE FOR IDE SUPPORT ONLY - DO NOT USE FOR BUILDS
// Actual builds use Bazel - run ./build.sh
// ============================================================

plugins {
    kotlin("jvm") version "2.1.0"
}

// Match Bazel's Java version
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
    
    // Logging (from maven_deps.json)
    implementation("org.slf4j:slf4j-api:2.0.16")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    
    // JSON (from maven_deps.json)
    implementation("com.google.code.gson:gson:2.13.1")
    
    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.2")
    
    // HTTP
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    
    // gRPC & Protobuf
    implementation("io.grpc:grpc-protobuf:1.77.0")
    implementation("io.grpc:grpc-services:1.77.0")
    implementation("com.google.protobuf:protobuf-java:4.31.1")
    
    // MapStruct
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    
    // Hytale Server API - load from local JAR if available
    compileOnly(files(rootProject.file(".local-assets/HytaleServer.jar")))
    
    // Common API library
    implementation(project(":libs:common-api"))
}

// Task to download sources for dependencies (enables Ctrl+Click)
tasks.register("downloadSources") {
    doLast {
        println("Run: ./gradlew build --refresh-dependencies")
        println("This downloads source JARs for Ctrl+Click navigation")
    }
}
