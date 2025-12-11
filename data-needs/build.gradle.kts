import energy.eddie.configureJavaCompileWithErrorProne
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("energy.eddie.java-conventions")

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "energy.eddie"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))

    implementation(libs.jackson.annotations)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.openapi.webmvc.ui)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.security)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.spring.boot.testcontainers)
    // needed to have access to RegionConnectorsCommonControllerAdvice that formats error responses correctly
    testImplementation(project(":region-connectors:shared"))

    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.h2database)
    testRuntimeOnly(libs.postgresql)
    testRuntimeOnly(libs.flyway.core)
    testRuntimeOnly(libs.flyway.postgresql)
}

tasks.test {
    useJUnitPlatform()
}

configureJavaCompileWithErrorProne("energy.eddie.dataneeds")

// disable bootJar task as it needs a main class and data-needs does not have one
tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
