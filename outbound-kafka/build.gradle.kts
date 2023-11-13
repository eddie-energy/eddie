import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

plugins {
    id("energy.eddie.java-conventions")
}

group = "energy.eddie"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(mapOf("path" to ":api")))
    implementation(libs.jakarta.annotation.api)
    implementation(libs.kafka.clients)
    implementation(libs.reactor.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.datatype.jdk8)
    implementation(libs.slf4j.simple)
    testImplementation(libs.junit.jupiter)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    if (!name.lowercase(Locale.getDefault()).contains("test")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "eddie.energy")
        }
    }
}