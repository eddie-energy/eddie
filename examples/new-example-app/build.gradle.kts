import energy.eddie.configureJavaCompileWithErrorProne

plugins {
    application
    id("energy.eddie.java-conventions")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "energy.eddie"
version = "2.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.thymeleaf)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("run-new-example-app", JavaExec::class) {
    mainClass.set("energy.eddie.NewExampleApp")
    classpath = sourceSets["main"].runtimeClasspath
    group = "development"
    description = "run the new example app with Spring"

    environment["NEWEXAMPLEAPP_PORT"] = 8082
}

configureJavaCompileWithErrorProne("energy.eddie.examples")
