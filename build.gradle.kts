plugins {
    id("java")
    id("jacoco")
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
    apply(plugin = "jacoco")

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

    apply(plugin = libraries.plugins.errorprone.get().pluginId)
}
