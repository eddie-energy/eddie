---
prev:
  text: "Quickstart"
  link: "./quickstart.md"
next:
  text: "API"
  link: "./api.md"
---

# Build and Setup

The region connectors are subprojects of the [region-connectors project](https://github.com/eddie-energy/eddie/tree/main/region-connectors).
Gradle with Kotlin is used to manage dependencies.
The following is a typical `build.gradle.kts` for a region connector:

```kotlin
import org.springframework.boot.gradle.tasks.bundling.BootJar
import energy.eddie.configureJavaCompileWithErrorProne

plugins {
    id("energy.eddie.java-conventions")
    id("energy.eddie.pnpm-build")

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "energy.eddie.regionconnector.foo.bar"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))
    implementation(project(":data-needs"))
    implementation(project(":region-connectors:shared"))
    implementation(libs.spring.boot.starter.web)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
}

// disable bootJar task as it needs a main class and region connectors do not have one
tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}

tasks.withType<Test> {
    useJUnitPlatform()
}

configureJavaCompileWithErrorProne("energy.eddie.regionconnector.foo.bar")

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    finalizedBy(tasks.jacocoTestCoverageVerification)
}
```

## Convention Plugins

There are two plugins and some custom functions available that add needed build tasks and common dependencies to the region connectors.

### Java Conventions

The java conventions plugin contains common plugins, dependencies, and plugin configurations needed by the region connectors.
It provides:

- jacoco
- java
- errorprone

### PNPM build

The `pnpm-build` plugin adds dependencies on pnpm builds, to automatically rebuild the custom elements of the region connectors and the EDDIE button.
Everytime the `javaCompile` task is executed,
`pnpm build` is also executed to ensure that the custom elements and EDDIE button are up to date.

### EddieExtensions

The EddieExtensions contains a function to configure errorprone, which can be called like this:

```kotlin
import energy.eddie.configureJavaCompileWithErrorProne
configureJavaCompileWithErrorProne("energy.eddie.regionconnector.foo.bar")
```
