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

configurations.all {
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "ch.qos.logback", module = "logback-core")
}

dependencies {
    implementation(project(":api"))
    implementation(project((":outbound-connectors:outbound-shared")))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.jakarta.annotation.api)

    implementation(libs.reactor.core)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.reactor.test)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.junit.mockito)
}

tasks.test {
    useJUnitPlatform()
}

configureJavaCompileWithErrorProne("energy.eddie.outbound.rest")


// disable bootJar task as it needs a main class and outbound connectors do not have one
tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}