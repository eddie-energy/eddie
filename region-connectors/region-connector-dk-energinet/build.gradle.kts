import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

plugins {
    id("energy.eddie.java-conventions")
    id("energy.eddie.pnpm-build")
    application
    id("org.openapi.generator") version "6.6.0"
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "energy.eddie"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(mapOf("path" to ":api")))
    implementation(project(mapOf("path" to ":region-connectors:shared")))
    implementation(libs.spring.boot.starter.web)

    // Required for openapi generator
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.databind.nullable)
    implementation(libs.jakarta.annotation.api)

    // Required for the generated API client
    implementation(libs.feign.core)
    implementation(libs.feign.okhttp)
    implementation(libs.feign.jackson)
    implementation(libs.feign.slf4j)
    implementation(libs.feign.form)

    // sl4j
    implementation(libs.slf4j.api)
    implementation(libs.log4j.sl4j2.impl)
    runtimeOnly(libs.log4j.jul)


    implementation(libs.reactor.core)

    implementation(libs.javalin)

    implementation(libs.microprofile.config)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.reactor.test)
}

configurations.all {
    // the aop package is already contained in spring-aop
    exclude(group = "aopalliance", module = "aopalliance")
    exclude(group = "commons-logging", module = "commons-logging") // TODO check
    exclude(group = "org.slf4j", module = "slf4j-simple") // TODO this shoudn't be necessary
    exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j2-impl") // TODO this neither
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

    library.set("feign")
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
            option("NullawayExcludedClasses=EnerginetCliClient.java")
        }
    }
}

application {
    mainClass.set("${customerApiPackagePrefix}.EnerginetCustomerCliClient")
    applicationDefaultJvmArgs = listOf("-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager")
}