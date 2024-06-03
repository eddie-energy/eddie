import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.util.*

plugins {
    id("energy.eddie.java-conventions")
    id("energy.eddie.pnpm-build")

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.openapi.generator)
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
    implementation(libs.hibernate.validator)
    implementation(libs.spring.boot.starter.webflux)

    implementation(libs.reactor.core)

    implementation(libs.nimbus.oidc)
    implementation(libs.bouncycastle.bcprov)
    implementation(libs.bouncycastle.bcpkix)

    runtimeOnly(libs.postgresql)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
    testImplementation(libs.okhttp3.mockwebserver)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.postgresql)
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

val packagePrefix = "energy.eddie.regionconnector.nl.mijn.aansluiting.client"

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("${projectDir}/src/main/resources/ReadingSeriesRetrieval_v0_3_1.json")
    outputDir.set(generatedSwaggerJavaDir)
    ignoreFileOverride.set("${projectDir}/src/main/resources/.openapi-generator-ignore")

    apiPackage.set("${packagePrefix}.api")
    invokerPackage.set("${packagePrefix}.invoker")
    modelPackage.set("${packagePrefix}.model")

    generateApiTests.set(false)
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
    library.set("webclient")
    cleanupOutput.set(true)
}

tasks.named("compileJava").configure {
    dependsOn(tasks.named("openApiGenerate"))
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