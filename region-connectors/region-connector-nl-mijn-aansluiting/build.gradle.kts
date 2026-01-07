import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.Verify
import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.util.*

plugins {
    id("energy.eddie.java-conventions")
    id("energy.eddie.pnpm-build")

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.undercouch.download)
}

group = "energy.eddie"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))
    implementation(project(":region-connectors:shared"))
    implementation(project(":data-needs"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.webclient)
    implementation(libs.jakarta.validation.api)

    implementation(libs.reactor.core)

    implementation(libs.nimbus.oidc)
    implementation(libs.bouncycastle.bcpkix)

    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.bouncycastle.bcprov)
    runtimeOnly(libs.hibernate.validator)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.reactor.test)
    testImplementation(libs.okhttp3.mockwebserver)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.spring.boot.testcontainers)

    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.spring.boot.starter.flyway)
    testRuntimeOnly(libs.flyway.postgresql)
    testRuntimeOnly(libs.postgresql)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.disableWarningsInGeneratedCode.set(true)
    val regexString = ".*/energy/eddie/regionconnector/nl/mijn/aansluiting/client/model/.*".replace("/", "[/\\\\]")
    options.errorprone.excludedPaths.set(regexString)
    if (!name.lowercase(Locale.getDefault()).contains("test")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "energy.eddie.regionconnector.nl.mijn.aansluiting")
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

// Directory for generated java files
val eanCodeboekOpenApiFileLocation = layout.buildDirectory.file("generated/sources/swagger/eancodeboek.json")
val generatedSwaggerJavaDir = "${layout.buildDirectory.asFile.get()}/generated/sources/swagger/main/java"

// Add generated sources to the main source set
sourceSets {
    main {
        java {
            srcDir(generatedSwaggerJavaDir)
        }
    }
    test {
        java {
            srcDir(generatedSwaggerJavaDir)
        }
    }
}

val eancodeboekUrl = "https://gateway.edsn.nl/eancodeboek/v3/api-docs"
val eancodeboekChecksum = "b84b118a7f3d11e49571bb4b1c2920c0"

val openApiDownloadTask = tasks.register<Download>("eancodeboekOpenApiDownload") {
    group = "download"
    description = "Download the Open API files for the EAN Codeboek API"
    src(eancodeboekUrl)
    dest(eanCodeboekOpenApiFileLocation)
    overwrite(false)
    onlyIfModified(true)
}

val eancodeboekVerifyTask = tasks.register<Verify>("verifyEANCodeboekOpenApiSpecs") {
    group = "openapi tools"
    description = "Verifies the downloaded OpenAPI specs"
    dependsOn(openApiDownloadTask)
    src(eanCodeboekOpenApiFileLocation)
    checksum(eancodeboekChecksum)
}

openApiGenerate {
    generatorName.set("java")
    inputSpec.set(eanCodeboekOpenApiFileLocation.get().asFile.absolutePath)
    outputDir.set(generatedSwaggerJavaDir)
    modelPackage.set("energy.eddie.regionconnector.nl.mijn.aansluiting.client.model")
    library.set("webclient")
    cleanupOutput.set(true)
    generateApiDocumentation.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    configOptions.set(
        mapOf(
            "sourceFolder" to "/",
            "useJakartaEe" to "true",
            "dateLibrary" to "java8"
        )
    )
    typeMappings.set(
        mapOf(
            "OffsetDateTime" to "ZonedDateTime"
        )
    )
    importMappings.set(
        mapOf(
            "java.time.OffsetDateTime" to "java.time.ZonedDateTime"
        )
    )
    globalProperties.set(
        mapOf(
            "apis" to "false",
            "invokers" to "false",
            "models" to ""
        )
    )
}

tasks.withType<GenerateTask>().configureEach {
    dependsOn(eancodeboekVerifyTask)
}

tasks.named("compileJava").configure {
    dependsOn(tasks.withType<GenerateTask>())
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    finalizedBy(tasks.jacocoTestCoverageVerification)
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude("energy.eddie.regionconnector.nl.mijn.aansluiting.client.model")
        }
    )
}