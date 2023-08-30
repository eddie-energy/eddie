import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("org.springframework.boot") version "3.1.2"
    id("io.spring.dependency-management") version "1.1.2"
    id("net.ltgt.errorprone") version "3.0.1"
    id("org.sonarqube") version "4.0.0.2929"
}

group = "energy.eddie.aiida"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // errorprone & nullAway
    annotationProcessor("com.uber.nullaway:nullaway:0.10.11")
    // Optional, some source of nullability annotations.
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    errorprone("com.google.errorprone:error_prone_core:2.18.0")
    errorproneJavac("com.google.errorprone:javac:9+181-r4173-1")

    testImplementation("org.testcontainers:postgresql:1.19.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    if (!name.lowercase().contains("test")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "energy.eddie")
        }
    }
}

tasks.named<Test>("test") {
    description = "Runs all tests except integration tests."
    useJUnitPlatform {
        filter {
            // exclude all integration tests
            excludeTestsMatching("*IntegrationTest")
        }
    }

    testLogging {
        events("passed")
    }
}

tasks.register<Test>("integrationTest") {
    description = "Runs only integration tests."
    group = "verification"
    useJUnitPlatform {
        filter {
            includeTestsMatching("*IntegrationTest")
        }
    }

    testLogging {
        events("passed")
    }
}
