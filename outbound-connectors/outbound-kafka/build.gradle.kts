import energy.eddie.configureJavaCompileWithErrorProne
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("energy.eddie.java-conventions")

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "energy.eddie"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))
    implementation(project((":outbound-connectors:outbound-shared")))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.kafka)

    implementation(libs.jakarta.annotation.api)

    implementation(libs.reactor.core)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.kafka.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.reactor.test)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.junit.mockito)

    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

configureJavaCompileWithErrorProne("energy.eddie.outbound.kafka")

// disable bootJar task as it needs a main class and outbound connectors do not have one
tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
