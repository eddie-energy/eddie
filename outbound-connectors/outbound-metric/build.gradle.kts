import energy.eddie.configureJavaCompileWithErrorProne
import net.ltgt.gradle.errorprone.errorprone
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.util.*

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
    implementation(libs.spring.boot.starter.webclient)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.reactor.core)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.reactor.test)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.okhttp3.mockwebserver)

    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.spring.boot.starter.flyway)
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

jsonSchema2Pojo {
    setSource(files("src/main/resources/schema"))
    targetPackage = "energy.eddie.outbound.metric.generated"
    targetDirectory = file("${project.layout.buildDirectory.asFile.get()}/generated/sources/schema/main/java")
    includeConstructors = true
    usePrimitives = true
    setAnnotationStyle("jackson")
}

sourceSets {
    main {
        java {
            srcDir("${layout.buildDirectory}/generated-sources")
        }
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn("generateJsonSchema2Pojo")
    source(jsonSchema2Pojo.targetDirectory)
}
