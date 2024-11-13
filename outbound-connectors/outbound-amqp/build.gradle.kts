import energy.eddie.configureJavaCompileWithErrorProne

plugins {
    id("energy.eddie.java-conventions")

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "energy.eddie"
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

    implementation(libs.spring.boot.starter.web)
    implementation(libs.jakarta.annotation.api)

    implementation(libs.reactor.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.datatype.jdk8)
    implementation(libs.jackson.dataformat.xml)
    implementation(libs.jackson.core)
    implementation(libs.slf4j.simple)


    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.reactor.test)
    testImplementation(libs.junit.mockito)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
configureJavaCompileWithErrorProne("energy.eddie.outbound.amqp")

