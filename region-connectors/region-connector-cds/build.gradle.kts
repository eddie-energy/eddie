import energy.eddie.configureJavaCompileWithErrorProne
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("energy.eddie.java-conventions")
    id("energy.eddie.pnpm-build")

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
    implementation(project(":data-needs"))
    implementation(project(":region-connectors:shared"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.security)

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

tasks.test {
    useJUnitPlatform()
}

configureJavaCompileWithErrorProne("energy.eddie.regionconnector.cds")

// disable bootJar task as it needs a main class and region connectors do not have one
tasks.getByName<BootJar>("bootJar") {
    enabled = false
}
