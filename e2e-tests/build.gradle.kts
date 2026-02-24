// SPDX-FileCopyrightText: 2024-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("java")
    id("energy.eddie.java-conventions")
}

group = "energy.eddie.tests.e2e"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.playwright)

    testImplementation(libs.jackson.databind)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    onlyIf {
        project.hasProperty("run-e2e-tests")
    }
}

tasks.register<JavaExec>("install-playwright-deps") {
    description = "Install playwright deps"
    group = "playwright"
    mainClass.set("com.microsoft.playwright.CLI")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("install", "--with-deps")
}
