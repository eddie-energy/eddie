import energy.eddie.configureJavaCompileWithErrorProne
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("energy.eddie.java-conventions")
    id("energy.eddie.pnpm-build")

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "energy.eddie.regionconnector.be.fluvius"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))
    implementation(project(":data-needs"))
    implementation(project(":region-connectors:shared"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.jackson.databind.nullable)

    implementation(libs.nimbus.oidc)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.okhttp3.mockwebserver)

    testRuntimeOnly(libs.postgresql)
    testRuntimeOnly(libs.flyway.core)
    testRuntimeOnly(libs.flyway.postgresql)
}

// disable bootJar task as it needs a main class and region connectors do not have one
tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    finalizedBy(tasks.jacocoTestCoverageVerification)
}

configureJavaCompileWithErrorProne("energy.eddie.regionconnector.be.fluvius")