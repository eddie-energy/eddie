import energy.eddie.configureJavaCompileWithErrorProne
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.io.FileOutputStream
import java.net.URI

plugins {
    id("energy.eddie.java-conventions")
    id("energy.eddie.pnpm-build")

    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "energy.eddie"
version = "1.0.0"

repositories {
    mavenCentral()
}

val pontonVersion = "4.6.5"
val pontonDestinationDir = layout.buildDirectory.dir("PontonXP-Messenger-${pontonVersion}-Linux")
// JAXB configuration holds classpath for running the JAXB XJC compiler
val jaxb: Configuration by configurations.creating
dependencies {
    implementation(project(":api"))
    implementation(project(":data-needs"))
    implementation(project(":region-connectors:shared"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.oxm)
    implementation(libs.nimbus.oidc)

    // dependency for PontonXP Messenger
    implementation(fileTree(pontonDestinationDir) { include("lib/adapterapi2.jar") })
    // dependencies needed to generate code
    jaxb(libs.jaxb.xjc)
    jaxb(libs.jaxb.runtime)
    jaxb(libs.jaxb.plugins)

    implementation(libs.jakarta.xml.bind.api)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.jakarta.validation.api)
    implementation(libs.commons.codec)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    implementation(libs.reactor.core)
    implementation(libs.opentelemetry.sdk.metrics)

    runtimeOnly(libs.hibernate.validator)
    runtimeOnly(libs.jaxb.runtime)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.reactor.test)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.okhttp3.mockwebserver)
    testImplementation(libs.xmlunit.core)
    testImplementation(libs.opentelemetry.sdk.testing)
    testRuntimeOnly(libs.flyway.core)
    testRuntimeOnly(libs.flyway.postgresql)
    testRuntimeOnly(libs.postgresql)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

// Directory for generated java files
val generatedXJCJavaDir = "${project.layout.buildDirectory.asFile.get().absolutePath}/generated/sources/xjc/main/java"

// Add generated sources to the main source set
sourceSets {
    main {
        java {
            srcDir(generatedXJCJavaDir)
        }
    }
    test {
        java {
            srcDir(generatedXJCJavaDir)
        }
    }
}

val generateEDASchemaClasses = tasks.register<JavaExec>("generateEDASchemaClasses") {
    description = "Generate EDA Java Classes from XSD files"
    group = "xml"
    classpath(jaxb)
    mainClass.set("com.sun.tools.xjc.XJCFacade")

    // make sure the directory exists
    file(generatedXJCJavaDir).mkdirs()

    // Path to XSD files
    val edaSchemaFiles = "src/main/schemas/eda/xsd/"

    // explicitly set the encoding because of rare issues discovered on Windows 10
    args(
        "-d",
        generatedXJCJavaDir,
        edaSchemaFiles,
        "-mark-generated",
        "-npa",
        "-encoding",
        "UTF-8",
        "-extension",
        "-Xfluent-api"
    )

    // Define the task inputs and outputs, so Gradle can track changes and only run the task when needed
    inputs.files(fileTree(edaSchemaFiles).include("**/*.xsd"))
    outputs.dir(generatedXJCJavaDir)
}


val pontonUri: URI =
    URI.create("https://www.ponton.de/downloads/xp/${pontonVersion}/PontonXP-Messenger-${pontonVersion}-Linux.zip")
val pontonDestinationFile = layout.buildDirectory.file("PontonXP-Messenger-${pontonVersion}-Linux.zip")
val pollProprietaryLibraries = task("pollProprietaryLibraries") {
    description = "Retrieves the adapter2 library for the Ponton X/P Messenger"
    group = "build"
    outputs.file(pontonDestinationFile)
    doLast {
        val outFile = pontonDestinationFile.get().asFile
        if (outFile.exists()) {
            println("File already exists, skipping download.")
            return@doLast
        }
        val client = HttpClients.createDefault()
        val get = HttpGet(pontonUri)
        val response = client.execute(get)
        if (response.statusLine.statusCode != HttpStatus.SC_OK) {
            throw IllegalStateException("Download of $pontonUri failed: ${response.statusLine}")
        }
        try {
            response.entity.writeTo(FileOutputStream(outFile))
        } finally {
            response.close()
        }
    }
}

val unpackProprietaryLibraries = task<Copy>("unpackProprietaryLibraries") {
    description = "Unpacks the adapter2 library for the Ponton X/P Messenger"
    group = "build"
    inputs.file(pontonDestinationFile)
    outputs.dir(pontonDestinationDir)
    dependsOn(pollProprietaryLibraries)
    from(zipTree(pontonDestinationFile))
    into(pontonDestinationDir)
    doLast {
        logger.info("Unpacked $pontonDestinationFile into $pontonDestinationDir")
    }
}

tasks.named("compileJava") {
    // generate the classes before compiling
    dependsOn(generateEDASchemaClasses)
    dependsOn(unpackProprietaryLibraries)
}

sourceSets.configureEach {
    java.srcDir(generateEDASchemaClasses.map { files() })
}

configureJavaCompileWithErrorProne("energy.eddie.regionconnector.at")

// exclude the following classes / packages from test coverage
val generatedSchemas = "at/ebutilities/schemata/*" // these files are generated by the generateEDASchemaClasses task

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    finalizedBy(tasks.jacocoTestCoverageVerification)
    classDirectories.setFrom(
        sourceSets.main.get().output.asFileTree.matching {
            exclude(generatedSchemas)
        }
    )
}

sonarqube {
    properties {
        property("sonar.coverage.exclusions", generatedSchemas)
        property(
            "sonar.cpd.exclusions",
            "**/at/eda/ponton/messages/**/*"
        ) // exclude files from duplication detection
    }
}

// disable bootJar task as it needs a main class and region connectors do not have one
tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
