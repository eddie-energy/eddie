import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

// Fixed in https://github.com/gradle/gradle/issues/22797
// Remove once Gradle 8.1+ is released: https://gradle.org/releases/
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("java")
    alias(libs.plugins.errorprone)
    alias(libs.plugins.sonarqube).apply(false)
}

repositories {
    mavenCentral()
}

group = "energy.eddie"
version = "0.0.0"

// for some reason libs is not available in allprojects scope
// when working with multiple projects, it's available if only the root project is present
// https://stackoverflow.com/questions/75640395/why-does-the-dependency-declared-in-libs-versions-toml-not-work-in-subprojects
val libraries = libs

// configure dependencies required by all projects
allprojects {
    plugins.withId(libraries.plugins.errorprone.get().pluginId) {
        dependencies {
            errorprone(libraries.errorprone.core)
        }
    }

    plugins.withType<JavaPlugin> {
        dependencies {
            annotationProcessor(libraries.nullaway)
            compileOnly(libraries.jsr305)
        }
    }
}

// make all projects use these plugins
allprojects {
    apply(plugin = libraries.plugins.sonarqube.get().pluginId)

    apply(plugin = libraries.plugins.errorprone.get().pluginId)
    tasks.withType<JavaCompile>().configureEach {
        if (!name.lowercase(Locale.getDefault()).contains("test")) {
            options.errorprone {
                check("NullAway", CheckSeverity.ERROR)
                // add namespaces we want to check for nullability
                option("NullAway:AnnotatedPackages", "energy.eddie")
            }
        }
    }
}
