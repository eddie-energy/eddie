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
    id("org.sonarqube")
}

dependencies {
    implementation(libs.jakarta.xml.bind.api)
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