import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

plugins {
    id("energy.eddie.java-conventions")
    id("energy.eddie.pnpm-build")
}

group = "energy.eddie"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(project(":api"))
    implementation(libs.javalin)
    compileOnly(libs.slf4j.api)
    implementation(libs.reactor.core)
    compileOnly(libs.jackson.annotations)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter)
    implementation(libs.microprofile.config)

}

tasks.getByName<Test>("test") {
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