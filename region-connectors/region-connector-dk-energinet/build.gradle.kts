import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

plugins {
    id("energy.eddie.java-conventions")
    application
    id("org.openapi.generator") version "6.6.0"
}

group = "energy.eddie"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)

    // Required for openapi generator
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.databind.nullable)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.apache.http.client)
    implementation(libs.apache.http.mime)

    // sl4j
    implementation(libs.slf4j.api)
    implementation(libs.log4j.sl4j2.impl)
    runtimeOnly(libs.log4j.jul)

    implementation(project(mapOf("path" to ":api")))
}

tasks.test {
    useJUnitPlatform()
}

// Directory for generated java files
val generatedSwaggerJavaDir = "${buildDir}/generated/sources/swagger/main/java"

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

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("${projectDir}/src/main/resources/energinet-api-client-v3.json")
    outputDir.set(generatedSwaggerJavaDir)
    ignoreFileOverride.set("${projectDir}/src/main/resources/.openapi-generator-ignore")
    
    apiPackage.set("${packagePrefix}.api")
    invokerPackage.set("${packagePrefix}.invoker")
    modelPackage.set("${packagePrefix}.model")

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
    if (!name.lowercase(Locale.getDefault()).contains("test") && !name.lowercase(Locale.getDefault()).contains("generated")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", packagePrefix)
            option("NullAway:UnannotatedClasses", "${packagePrefix}.api.MeteringDataApi")
            option("NullAway:UnannotatedClasses", "${packagePrefix}.api.AuthorizationApi")
            option("NullAway:TreatGeneratedAsUnannotated", true)
            // Regex fits to Windows and Unix-style path separators. CAVEAT: excludedPaths needs a regex string!
            val regexString = ".*/energy/eddie/regionconnector/dk/energinet/invoker/.*".replace("/", "[/\\\\]")
            this.excludedPaths.set(regexString)
            option("NullawayExcludedClasses=EnedisApiClient.java")
        }
    }
}