import energy.eddie.configureJavaCompileWithErrorProne
import net.ltgt.gradle.errorprone.errorprone
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.util.Locale

plugins {
    id("energy.eddie.java-conventions")

    alias(libs.plugins.jsonschema2pojo)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "energy.eddie"
version = "0.0.0"

repositories {
    mavenCentral()
}

configurations.all {
    exclude(group = "ch.qos.logback", module = "logback-classic")
    exclude(group = "ch.qos.logback", module = "logback-core")
}

dependencies {
    implementation(project(":api"))
    implementation(project(":data-needs"))
    implementation(project(":outbound-connectors:outbound-shared"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.reactor.core)
    runtimeOnly(libs.slf4j.simple)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.reactor.test)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.okhttp3.mockwebserver)

    testRuntimeOnly(libs.flyway.core)
    testRuntimeOnly(libs.flyway.postgresql)
    testRuntimeOnly(libs.postgresql)
}

tasks.test {
    useJUnitPlatform()
}

configureJavaCompileWithErrorProne("energy.eddie.outbound.metric")


// disable bootJar task as it needs a main class and outbound connectors do not have one
tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.disableWarningsInGeneratedCode.set(true)
    if (!name.lowercase(Locale.getDefault()).contains("test")) {
        options.errorprone {
            option("NullAway:TreatGeneratedAsUnannotated", true)
        }
    }
}

// Define a single, consistent generated sources directory using DirectoryProperty
val generatedSchemaSourcesDir = layout.buildDirectory.dir("generated/sources/schema/main/java")

jsonSchema2Pojo {
    setSource(files("src/main/resources/schema"))
    targetPackage = "energy.eddie.outbound.metric.generated"
    // Use DirectoryProperty instead of interpolating buildDirectory to a String
    targetDirectory = generatedSchemaSourcesDir.get().asFile
    includeConstructors = true
    usePrimitives = true
    setAnnotationStyle("jackson")
}

sourceSets {
    named("main") {
        java {
            // Point main Java source set to the same generated directory
            srcDir(generatedSchemaSourcesDir)
        }
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn("generateJsonSchema2Pojo")
    // Ensure compileJava also sees the generated sources
    source(generatedSchemaSourcesDir)
}
