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

tasks.register<Copy>("copyAdminConsoleUi") {
    group = "build"
    description = "Copies AIIDA UI build output to public resources"
    dependsOn(":pnpmBuildAdminConsole")
    from("ui/dist")
    into("src/main/resources/public")
}

tasks.register<Copy>("copyAdminConsoleUiIndex") {
    group = "build"
    description = "Copies AIIDA UI index page to Thymeleaf template directory"
    dependsOn(":pnpmBuildAdminConsole")
    from("ui/dist/index.html")
    into("src/main/resources/templates")
}

tasks.register("buildAdminConsoleUi") {
    group = "build"
    description = "Builds the AIIDA UI into the Spring application"
    dependsOn(":pnpmBuildAdminConsole")
    dependsOn("copyAdminConsoleUi")
    dependsOn("copyAdminConsoleUiIndex")
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn("buildAdminConsoleUi")
}

tasks.withType<ProcessResources>().configureEach {
    dependsOn("buildAdminConsoleUi")
}

tasks.clean {
    delete("src/main/resources/public")
    delete("src/main/resources/templates/index.html")
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
