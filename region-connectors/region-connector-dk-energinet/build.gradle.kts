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

    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.undercouch.download)
}

group = "energy.eddie"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))
    implementation(project(":data-needs"))
    implementation(project(":region-connectors:shared"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webclient)
    implementation(libs.spring.boot.starter.actuator)

    // Required for openapi generator
    implementation(libs.jackson.databind)
    implementation(libs.jackson.databind.nullable)
    implementation(libs.jakarta.annotation.api)

    implementation(libs.reactor.core)

    implementation(libs.nimbus.oidc)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.reactor.test)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.okhttp3.mockwebserver)

    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

// Directory for generated java files
val generatedSwaggerJavaDir = layout.buildDirectory.dir("generated/sources/swagger/main/java")

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

val packagePrefix = "energy.eddie.regionconnector.dk.energinet"
val customerApiPackagePrefix = "${packagePrefix}.customer"
val energinetOpenApiChecksum = "8d08c17a4cb68be1e3b7d34c3858fbbd"
val energinetOpenApiFileLocation = layout.buildDirectory.file("generated/sources/swagger/energinet.json")

val downloadTask = tasks.register<Download>("openApiDownload") {
    group = "download"
    description = "Downloads the OpenAPI specs for energinet"
    src("https://api.eloverblik.dk/customerapi/swagger/customerapi-v1.0/swagger.json")
    dest(energinetOpenApiFileLocation)
    overwrite(false)
    onlyIfModified(true)
}

val verificationTask = tasks.register<Verify>("verifyOpenApiSpecs") {
    description = "Validates downloaded OpenAPI specs"
    group = "verification"
    dependsOn(downloadTask)
    src(energinetOpenApiFileLocation)
    checksum(energinetOpenApiChecksum)
}

tasks.withType<GenerateTask>().configureEach {
    dependsOn(verificationTask)
}

openApiGenerate {
    generatorName.set("java")
    inputSpec.set(energinetOpenApiFileLocation.get().asFile.absolutePath)
    outputDir.set(generatedSwaggerJavaDir.get().asFile.absolutePath)
    ignoreFileOverride.set("${projectDir}/src/main/resources/.openapi-generator-ignore")

    modelPackage.set("${customerApiPackagePrefix}.model")

    generateApiTests.set(false)
    generateApiDocumentation.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    configOptions.set(
        mapOf(
            "sourceFolder" to "/",
            "useJakartaEe" to "true",
            "dateLibrary" to "java8",
            "openApiNullable" to " false"
        )
    )
    globalProperties.set(
        mapOf(
            "apis" to "false",
            "invokers" to "false",
            "models" to ""
        )
    )

    library.set("webclient")
    cleanupOutput.set(true)
}

tasks.named("compileJava").configure {
    dependsOn(tasks.withType<GenerateTask>())
}

sourceSets.configureEach {
    java.srcDir(tasks.withType<GenerateTask>().map { files() })
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.disableWarningsInGeneratedCode.set(true)
    // Regex fits to Windows and Unix-style path separators. CAVEAT: excludedPaths needs a regex string!
    val regexString = ".*/energy/eddie/regionconnector/dk/energinet/customer/(model|invoker)/.*".replace("/", "[/\\\\]")
    options.errorprone.excludedPaths.set(regexString)
    if (!name.lowercase(Locale.getDefault()).contains("test") && !name.lowercase(Locale.getDefault())
            .contains("generated")
    ) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", packagePrefix)
            option("NullAway:UnannotatedClasses", "${customerApiPackagePrefix}.model")
            option("NullAway:TreatGeneratedAsUnannotated", true)
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
