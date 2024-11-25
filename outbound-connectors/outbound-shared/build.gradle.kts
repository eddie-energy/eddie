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
    implementation(libs.jaxb.runtime)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.datatype.jdk8)
    implementation(libs.jackson.dataformat.xml)
    implementation(libs.jackson.jakarta.xmlbind.annotations)
    implementation(libs.jackson.core)

    testImplementation(libs.junit.jupiter)
    testImplementation(platform(libs.junit.bom))
}

tasks.test {
    useJUnitPlatform()
}

configureJavaCompileWithErrorProne("energy.eddie.outbound.shared")
