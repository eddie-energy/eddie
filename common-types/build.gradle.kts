// SPDX-FileCopyrightText: 2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("java")
    jacoco
}

group = "energy.eddie"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":cim"))
    implementation(libs.jakarta.persistence.api)
    implementation(libs.jakarta.validation.api)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.jackson.annotations)
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.withType<JacocoReport>())
}

tasks.withType<JacocoReport> {
    reports {
        xml.required.set(true)
    }
}
