// SPDX-FileCopyrightText: 2023-2025 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie

import org.gradle.accessors.dm.LibrariesForLibs

val libs = the<LibrariesForLibs>()

repositories {
    mavenCentral()
}

plugins {
    java
    jacoco
    id("net.ltgt.errorprone")
}

dependencies {
    // Shared between region-connectors
    implementation(libs.jakarta.xml.bind.api)
    implementation(libs.swagger.annotations)
    implementation(project(":cim"))
}

plugins.withId(libs.plugins.errorprone.get().pluginId) {
    dependencies {
        errorprone(libs.errorprone.core)
    }
}

plugins.withType<JavaPlugin> {
    dependencies {
        annotationProcessor(libs.nullaway)
        compileOnly(libs.jsr305)
    }
}

jacoco {
    toolVersion = "0.8.9"
}

tasks.withType<Test>().configureEach {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.withType<JacocoReport> {
    reports {
        xml.required.set(true)
    }
}