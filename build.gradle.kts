import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

// Fixed in https://github.com/gradle/gradle/issues/22797
// Remove once Gradle 8.1+ is released: https://gradle.org/releases/
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("java")
    alias(libs.plugins.errorprone)
}

repositories {
    mavenCentral()
}

group = "energy.eddie"
version = "0.0.0"

dependencies {
    compileOnly(libs.errorprone.core)
    annotationProcessor(libs.nullaway)
    compileOnly(libs.jsr305)
}

allprojects {
    tasks.withType<JavaCompile>() {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
        }
    }
}