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

## Convention Plugins and Helper Scripts

There are two plugins and some custom functions available that add required build tasks and common dependencies to the region connectors.

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

> [!INFO]
> It is only required if the region connector uses pnpm to build the custom element.

### EddieExtensions

The EddieExtensions script contains a function to configure errorprone, which can be called like this:

```kotlin
import energy.eddie.configureJavaCompileWithErrorProne
configureJavaCompileWithErrorProne("energy.eddie.regionconnector.foo.bar")
```

## Dependencies and Plugins

Dependencies and plugins are defined using the [`libs.versions.toml`](https://github.com/eddie-energy/eddie/blob/main/gradle/libs.versions.toml) syntax provided by [Gradle version catalogues](https://docs.gradle.org/current/userguide/version_catalogs.html).
