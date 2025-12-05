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
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.security)
    implementation(libs.flyway.core)

    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.spring.boot.testcontainers)

    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)

    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register("run-new-example-app", JavaExec::class) {
    mainClass.set("energy.eddie.examples.newexampleapp.NewExampleApp")
    classpath = sourceSets["main"].runtimeClasspath
    group = "development"
    description = "run the new example app with Spring"

    environment["NEWEXAMPLEAPP_PORT"] = 8082

    // when using PostgreSQL
    environment["JDBC_URL"] = "jdbc:postgresql://localhost:5432/new_example_app"
    environment["JDBC_USER"] = "test"
    environment["JDBC_PASSWORD"] = "test"
}

configureJavaCompileWithErrorProne("energy.eddie.examples")
