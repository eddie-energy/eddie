import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.util.*

plugins {
    id("energy.eddie.java-conventions")
    id("energy.eddie.pnpm-build")

    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "energy.eddie"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))
    implementation(project(":region-connectors:shared"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webflux)

    // Required for openapi generator
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.databind.nullable)
    implementation(libs.jakarta.annotation.api)

    implementation(libs.reactor.core)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.reactor.test)
    testImplementation(libs.spring.boot.starter.test)
}

tasks.test {
    useJUnitPlatform()
}

// Directory for generated java files
val generatedSwaggerJavaDir = "${project.layout.buildDirectory.asFile.get()}/generated/sources/swagger/main/java"

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

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("${projectDir}/src/main/resources/energinet-customer-api-client-v3.json")
    outputDir.set(generatedSwaggerJavaDir)
    ignoreFileOverride.set("${projectDir}/src/main/resources/.openapi-generator-ignore")

    apiPackage.set("${customerApiPackagePrefix}.api")
    invokerPackage.set("${customerApiPackagePrefix}.invoker")
    modelPackage.set("${customerApiPackagePrefix}.model")

    generateApiTests.set(false)
    generateApiDocumentation.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    configOptions.set(mapOf(
            "sourceFolder" to "/",
            "useJakartaEe" to "true",
            "dateLibrary" to "java8",
    ))

    library.set("webclient")
    cleanupOutput.set(true)
}

tasks.named("compileJava").configure {
    dependsOn(tasks.named("openApiGenerate"))
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.disableWarningsInGeneratedCode.set(true)
    // Regex fits to Windows and Unix-style path separators. CAVEAT: excludedPaths needs a regex string!
    val regexString = ".*/energy/eddie/regionconnector/dk/energinet/customer/(model|invoker)/.*".replace("/", "[/\\\\]")
    options.errorprone.excludedPaths.set(regexString)
    if (!name.lowercase(Locale.getDefault()).contains("test") && !name.lowercase(Locale.getDefault()).contains("generated")) {
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
