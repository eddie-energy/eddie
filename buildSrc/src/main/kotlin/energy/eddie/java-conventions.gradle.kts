// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie

repositories {
    mavenCentral()
}

plugins {
    java
    jacoco
    id("net.ltgt.errorprone")
}

val libs = extensions
    .getByType<VersionCatalogsExtension>()
    .named("libs")

dependencies {
    // Shared between region-connectors
    implementation(libs.findLibrary("jakarta-xml-bind-api").get())
    implementation(libs.findLibrary("swagger-annotations").get())
    implementation(project(":cim"))
    implementation(project(":data-need-api"))
    implementation(project(":common-types"))
}

// Security patches not yet included in the Spring Boot 4.1.1 BOM.
project.extensions.extraProperties["tomcat.version"] = "11.0.27"
project.extensions.extraProperties["jackson-bom.version"] = "3.1.8"
project.extensions.extraProperties["jackson-2-bom.version"] = "2.22.4"

plugins.withId("net.ltgt.errorprone") {
    dependencies {
        errorprone(libs.findLibrary("errorprone-core").get())
    }
}

plugins.withType<JavaPlugin> {
    dependencies {
        annotationProcessor(libs.findLibrary("nullaway").get())
        compileOnly(libs.findLibrary("jsr305").get())
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