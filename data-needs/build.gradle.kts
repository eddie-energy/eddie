import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.util.*

plugins {
    id("energy.eddie.java-conventions")

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "energy.eddie"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))

    implementation(libs.jackson.annotations)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.spring.boot.starter.test)
    // needed to have access to RegionConnectorsCommonControllerAdvice that formats error responses correctly
    testImplementation(project(":region-connectors:shared"))

    testRuntimeOnly(libs.h2database)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    if (!name.lowercase(Locale.getDefault()).contains("test")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "energy.eddie")
        }
    }
}

// disable bootJar task as it needs a main class and data-needs does not have one
tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
