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
version = "2.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))
    implementation(project(":outbound-connectors:outbound-shared"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.spring.boot.security)
    implementation(libs.spring.boot.starter.oauth2.client)

    implementation(libs.reactor.core)

    testImplementation(libs.spring.security.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.h2database)
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(":pnpmBuildAdminConsole")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    if (!name.lowercase(Locale.getDefault()).contains("test")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "energy.eddie.outbound.admin.console")
        }
    }
}

// disable bootJar task as it needs a main class and region connectors do not have one
tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
