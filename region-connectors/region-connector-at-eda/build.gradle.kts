import de.undercouch.gradle.tasks.download.Download
import de.undercouch.gradle.tasks.download.VerifyAction
import energy.eddie.configureJavaCompileWithErrorProne
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.net.URI
import java.util.zip.ZipFile

plugins {
    id("energy.eddie.java-conventions")
    id("energy.eddie.pnpm-build")

    alias(libs.plugins.undercouch.download)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "energy.eddie"
version = "1.0.0"

repositories {
    mavenCentral()
}

// This section defines the different versions of the ebutilities schemas that are currently used by the EDA Region Connector.
// The checksums are used to guarantee stable schemas.
// They are generated using the md5sum.

val cmNotificationVersions = mapOf(
    "01p12" to "7e9b168104f25071b92a7d894d0d1249", // current
    "01p20" to "1cc5bca4a252263039b4617a68686df3" // active from 2026-04-13
)
val cmRequestVersions = mapOf(
    "01p21" to "5e0c0e505e9ffe4e36125434702ef3c4", // current
    "01p30" to "97c03cb478e13b0ecd744a5804b289c1" // active from 2026-04-13
)
val cmRevokeVersions = mapOf(
    "01p00" to "ea294e7dd7df3d806a5221632bcfdbb8", // current
    "01p10" to "ddcfcad87aca9751c227230e7d523371" // active from 2026-04-13
)
val consumptionRecordVersions = mapOf("01p41" to "256671cf2e7ccd736d6b1ec379177e6e")
val cpCommonTypesVersions = mapOf("01p20" to "2482726eb973718551dcb006b3a65405")
val cpNotificationVersions = mapOf("01p13" to "fcbbc0f86b0b3da5a7f7c154176b132c")
val cpRequestVersions = mapOf("01p12" to "eb503977846a069ec0b68f5f3104e2fa")
val masterDataVersions = mapOf(
    "01p32" to "98dda83247de9459de3a7a588992056c", // current
    "01p33" to "100f8e7b4c8c836ed2d852a9d035e460" // current
)

val edaVersionMatrix: Map<String, Map<String, String>> = mapOf(
    "https://www.ebutilities.at/schemata/customerconsent/cmnotification/%s/CMNotification_%s.xsd" to cmNotificationVersions,
    "https://www.ebutilities.at/schemata/customerconsent/cmrequest/%s/CMRequest_%s.xsd" to cmRequestVersions,
    "https://www.ebutilities.at/schemata/customerconsent/cmrevoke/%s/CMRevoke_%s.xsd" to cmRevokeVersions,
    "https://www.ebutilities.at/schemata/customerprocesses/consumptionrecord/%s/ConsumptionRecord_%s.xsd" to consumptionRecordVersions,
    "https://www.ebutilities.at/schemata/customerprocesses/common/types/%s/CPCommonTypes_%s.xsd" to cpCommonTypesVersions,
    "https://www.ebutilities.at/schemata/customerprocesses/cpnotification/%s/CPNotification_%s.xsd" to cpNotificationVersions,
    "https://www.ebutilities.at/schemata/customerprocesses/cprequest/%s/CPRequest_%s.xsd" to cpRequestVersions,
    "https://www.ebutilities.at/schemata/customerprocesses/masterdata/%s/MasterData_%s.xsd" to masterDataVersions,
)

val pontonVersion = "4.6.5"
val pontonLib = layout.projectDirectory.file("libs/adapterapi2.jar")
// JAXB configuration holds classpath for running the JAXB XJC compiler
val jaxb: Configuration by configurations.creating
dependencies {
    implementation(project(":api"))
    implementation(project(":data-needs"))
    implementation(project(":region-connectors:shared"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.webclient)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.oxm)
    implementation(libs.nimbus.oidc)

    // dependency for PontonXP Messenger
    implementation(files("libs/adapterapi2.jar"))
    // dependencies needed to generate code
    jaxb(libs.jaxb.xjc)
    jaxb(libs.jaxb.runtime)
    jaxb(libs.jaxb.plugins)

    implementation(libs.jakarta.xml.bind.api)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.jakarta.validation.api)
    implementation(libs.commons.codec)

    implementation(libs.jackson.databind)

    implementation(libs.reactor.core)
    implementation(libs.opentelemetry.sdk.metrics)

    runtimeOnly(libs.hibernate.validator)
    runtimeOnly(libs.jaxb.runtime)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.reactor.test)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.okhttp3.mockwebserver)
    testImplementation(libs.xmlunit.core)
    testImplementation(libs.opentelemetry.sdk.testing)

    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.spring.boot.starter.flyway)
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

// Path to XSD files
val edaSchemaPath = "schemas/eda/xsd/"
val edaSchemaDir = layout.buildDirectory.dir(edaSchemaPath)
val sources: List<Pair<URI, String>> = edaVersionMatrix.flatMap { (key, value) ->
    value.map { (version, checksum) ->
        URI.create(key.format(version, version)) to checksum
    }
}
val downloadEDASchemas = tasks.register<Download>("downloadEDASchemas") {
    description = "Downloads EDA XML Schemas"
    group = "download"
    src(sources.map { it.first })
    dest(edaSchemaDir)
    overwrite(false)
    onlyIfModified(false)
}

val validatedEDASchemas = tasks.register("verifyEDASchemas") {
    description = "Verifies the EDA Schemas"
    group = "download"
    dependsOn(downloadEDASchemas)
    inputs.dir(edaSchemaDir)
    doLast {
        if (!downloadEDASchemas.get().didWork)
            return@doLast
        val verify = VerifyAction(layout)
        sources.forEach { (url, checksum) ->
            val file = file(url.toURL().file)
            verify.src(layout.buildDirectory.file("$edaSchemaPath${file.name}"))
            verify.checksum(checksum)
            verify.execute()
        }
    }
}

val generateEDASchemaClasses = tasks.register<JavaExec>("generateEDASchemaClasses") {
    dependsOn(validatedEDASchemas)
    description = "Generate EDA Java Classes from XSD files"
    group = "xml"
    classpath(jaxb)
    mainClass.set("com.sun.tools.xjc.XJCFacade")

    // make sure the directory exists
    file(generatedXJCJavaDir).mkdirs()

    // explicitly set the encoding because of rare issues discovered on Windows 10
    args(
        "-d",
        generatedXJCJavaDir,
        edaSchemaDir.get(),
        "-mark-generated",
        "-npa",
        "-encoding",
        "UTF-8",
        "-extension",
        "-Xfluent-api"
    )

    // Define the task inputs and outputs, so Gradle can track changes and only run the task when needed
    inputs.dir(edaSchemaDir)
    outputs.dir(generatedXJCJavaDir)
}


val pontonUri: URI =
    URI.create("https://www.ponton.de/downloads/xp/${pontonVersion}/PontonXP-Messenger-${pontonVersion}-Linux.zip")
val pontonDestinationFile = layout.projectDirectory.file("libs/PontonXP-Messenger-${pontonVersion}-Linux.zip")
if (!pontonLib.asFile.exists()) {
    download.run {
        src(pontonUri)
        dest(pontonDestinationFile)
        overwrite(false)
        onlyIfModified(true)
    }
    ZipFile(pontonDestinationFile.asFile).use { zip ->
        val entry = zip.getEntry("lib/adapterapi2.jar")
        logger.lifecycle("Extracting $entry")
        zip.getInputStream(entry).use { input ->
            val destFile = pontonLib.asFile
            destFile.parentFile.mkdirs()
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}

tasks.named("compileJava") {
    // generate the classes before compiling
    dependsOn(generateEDASchemaClasses)
}

tasks.withType<Javadoc> {
    dependsOn(generateEDASchemaClasses)
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
