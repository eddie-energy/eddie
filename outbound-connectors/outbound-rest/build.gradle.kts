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
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.websocket)
    implementation(libs.spring.oxm)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.jackson.jakarta.xmlbind.annotations)
    implementation(libs.jackson.dataformat.xml)
    implementation(libs.reactor.core)
    runtimeOnly(libs.slf4j.simple)
    runtimeOnly(libs.jaxb.runtime)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.reactor.test)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testRuntimeOnly(libs.flyway.core)
    testRuntimeOnly(libs.flyway.postgresql)
    testRuntimeOnly(libs.postgresql)
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