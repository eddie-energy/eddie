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

    testImplementation(libs.junit.jupiter)
    testImplementation(platform(libs.junit.bom))
}

tasks.test {
    useJUnitPlatform()
}

configureJavaCompileWithErrorProne("energy.eddie.outbound.shared")
