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
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.hibernate.validator)
    implementation(libs.spring.retry)
    implementation(libs.spring.aspects)

    // Required for openapi generator
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.databind.nullable)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.apache.http.client)
    implementation(libs.apache.http.mime)

    implementation(libs.reactor.core)

    testImplementation(libs.reactor.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.spring.boot.starter.test)
}

tasks.getByName<Test>("test") {
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

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("${projectDir}/src/main/resources/enedis-api-client-v3.json")
    outputDir.set(generatedSwaggerJavaDir)
    ignoreFileOverride.set("${projectDir}/src/main/resources/.openapi-generator-ignore")

    apiPackage.set("energy.eddie.regionconnector.fr.enedis.api")
    invokerPackage.set("energy.eddie.regionconnector.fr.enedis.invoker")
    modelPackage.set("energy.eddie.regionconnector.fr.enedis.model")

    generateApiTests.set(false)
    generateModelTests.set(false)
    configOptions.set(mapOf(
            "sourceFolder" to "/",
            "useJakartaEe" to "true",
            "dateLibrary" to "java8",
    ))

    library.set("native")
    cleanupOutput.set(true)
}

tasks.named("compileJava").configure {
    dependsOn(tasks.named("openApiGenerate"))
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.disableWarningsInGeneratedCode.set(true)
    if (!name.lowercase(Locale.getDefault()).contains("test") && !name.lowercase(Locale.getDefault()).contains("generated")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "energy.eddie.regionconnector.fr.enedis")
            option("NullAway:UnannotatedClasses", "energy.eddie.regionconnector.fr.enedis.api.MeteringDataApi")
            option("NullAway:UnannotatedClasses", "energy.eddie.regionconnector.fr.enedis.api.AuthorizationApi")
            option("NullAway:TreatGeneratedAsUnannotated", true)
            // Regex fits to Windows and Unix-style path separators. CAVEAT: excludedPaths needs a rexex string!
            val regexString = ".*/energy/eddie/regionconnector/fr/enedis/invoker/.*".replace("/", "[/\\\\]")
            this.excludedPaths.set(regexString)
            option("NullawayExcludedClasses=EnedisApiClient.java")
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