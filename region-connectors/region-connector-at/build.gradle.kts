import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

plugins {
    id("energy.eddie.java-conventions")
    application
}

group = "energy.eddie"
version = "0.0.0"

repositories {
    mavenCentral()
}


// JAXB configuration holds classpath for running the JAXB XJC compiler
val jaxb: Configuration by configurations.creating

dependencies {
    implementation(project(":api"))
    testImplementation(libs.junit.jupiter)

    // dependency for PontonXP Messenger
    implementation(files("libs/adapterapi2.jar"))
    // dependencies needed to generate code
    jaxb(libs.jaxb.xjc)
    jaxb(libs.jaxb.runtime)

    implementation(libs.jaxb.runtime)
    implementation(libs.jakarta.xml.bind.api)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.commons.codec)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.slf4j.api)

    runtimeOnly(libs.log4j.sl4j2.impl)
    runtimeOnly(libs.log4j.api)
    runtimeOnly(libs.log4j.core)
    runtimeOnly(libs.log4j.jul)

    implementation(libs.reactor.core)
    testImplementation(libs.reactor.test)

    implementation(libs.javalin)
}


tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

// Directory for generated java files
val generatedXJCJavaDir = "${buildDir.absolutePath}/generated/sources/xjc/main/java"

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

val generateEDASchemaClasses = tasks.create<JavaExec>("generateEDASchemaClasses") {
    description = "Generate EDA Java Classes from XSD files"
    classpath(jaxb)
    mainClass.set("com.sun.tools.xjc.XJCFacade")

    // make sure the directory exists
    file(generatedXJCJavaDir).mkdirs()

    // Path to XSD files
    val edaSchemaFiles = "src/main/schemas/eda/xsd/"

    args("-d", generatedXJCJavaDir, edaSchemaFiles, "-mark-generated", "-npa")

    // Define the task inputs and outputs, so Gradle can track changes and only run the task when needed
    inputs.files(fileTree(edaSchemaFiles).include("**/*.xsd"))
    outputs.dir(generatedXJCJavaDir)
}


tasks.named("compileJava") {
    // generate the classes before compiling
    dependsOn(generateEDASchemaClasses)
}

tasks.withType<JavaCompile>().configureEach {
    if (!name.lowercase(Locale.getDefault()).contains("test")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "energy.eddie.regionconnector.at")
        }
    }
}

application {
    mainClass.set("energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterCliClient")
    applicationDefaultJvmArgs = listOf("-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}