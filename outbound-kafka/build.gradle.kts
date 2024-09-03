import energy.eddie.configureJavaCompileWithErrorProne

plugins {
    id("energy.eddie.java-conventions")
}

group = "energy.eddie"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))

    implementation(libs.jakarta.annotation.api)
    implementation(libs.kafka.clients)
    implementation(libs.reactor.kafka)

    implementation(libs.reactor.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.datatype.jdk8)
    implementation(libs.slf4j.simple)


    testImplementation(libs.junit.jupiter)
    testImplementation(libs.reactor.test)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.junit.mockito)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

configureJavaCompileWithErrorProne("energy.eddie.outbound.kafka")
