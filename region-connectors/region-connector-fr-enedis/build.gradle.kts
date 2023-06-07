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
    implementation(libs.dotenv)

    // Required for openapi generator
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.databind.nullable)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.apache.http.client)
    implementation(libs.apache.http.mime)

    implementation(libs.slf4j.api)

    // sl4j
    implementation(libs.log4j.sl4j2.impl)

    runtimeOnly(libs.slf4j.api)
    runtimeOnly(libs.log4j.sl4j2.impl)
    runtimeOnly(libs.log4j.jul)

    implementation(project(mapOf("path" to ":region-connectors:region-connector-api")))
}

tasks.getByName<Test>("test") {
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
    configOptions.set(mapOf("sourceFolder" to "/", "useJakartaEe" to "true", "dateLibrary" to "java8"))

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
            option("NullAway:AnnotatedPackages", "energy.eddie.regionconnector.fr.enedis.client")
        }
    }
}

application {
    mainClass.set("energy.eddie.regionconnector.fr.enedis.Main")
    applicationDefaultJvmArgs = listOf("-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager")
}