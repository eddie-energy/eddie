import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

plugins {
    id("energy.eddie.java-conventions")
    application
    id("org.gradlex.extra-java-module-info") version "1.3"

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
    testImplementation(libs.junit.mockito)

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

    // sl4j
    implementation(libs.slf4j.api)
    implementation(libs.log4j.sl4j2.impl)
    runtimeOnly(libs.log4j.jul)

    implementation(libs.reactor.core)
    testImplementation(libs.reactor.test)

    implementation(libs.javalin)

    implementation(libs.microprofile.config)
}

extraJavaModuleInfo {
    // These are needed, because the plugin extra-java-module-info would complain about them
    automaticModule("org.jetbrains:annotations", "org.jetbrains.annotations")
    automaticModule("org.jetbrains.kotlin:kotlin-stdlib-common", "kotlin.stdlib")
    automaticModule("javax.inject:javax.inject", "javax.inject")
    automaticModule("aopalliance:aopalliance", "aopalliance")
    automaticModule("com.google.guava:failureaccess", "failureaccess")
    automaticModule("com.google.guava:listenablefuture", "listenablefuture")
    automaticModule("com.google.code.findbugs:jsr305", "com.google.code.findbugs.jsr305")
    automaticModule("com.google.j2objc:j2objc-annotations", "j2objc.annotations")

    // stuff that comes from the convention plugin, only errorprone.annotations has a set automatic module name, the others haven't
    automaticModule("com.google.errorprone:error_prone_annotation", "com.google.errorprone.annotations")
    automaticModule("com.uber.nullaway:nullaway", "com.uber.nullaway:nullaway")
    automaticModule("com.google.auto:auto-common", "com.google.auto:auto-common")
    automaticModule("com.google.auto.value:auto-value-annotations", "com.google.auto.value:auto-value-annotations")
    automaticModule("com.google.errorprone:error_prone_core", "com.google.errorprone:error_prone_core")
    automaticModule("com.google.errorprone:error_prone_check_api", "com.google.errorprone:error_prone_check_api")
    automaticModule("com.google.errorprone:error_prone_type_annotations", "com.google.errorprone:error_prone_type_annotations")
    automaticModule("org.pcollections:pcollections", "org.pcollections:pcollections")
    automaticModule("com.github.kevinstern:software-and-algorithms", "com.github.kevinstern:software-and-algorithms")
    automaticModule("org.eclipse.jgit:org.eclipse.jgit", "org.eclipse.jgit:org.eclipse.jgit")

    automaticModule("org.eclipse.microprofile.config:microprofile-config-api", "eclipse.microprofile.config.api")
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
    mainClass.set("energy.eddie.regionconnector.at.eda.EdaRegionConnector")
    applicationDefaultJvmArgs = listOf("-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager")
}

tasks.named<JavaExec>("run") {
    standardInput = System.`in`
}

// create additional run task
tasks.register<JavaExec>("runCliClient") {
    group = "application"
    standardInput = System.`in`
    mainClass.set("energy.eddie.regionconnector.at.eda.ponton.PontonXPAdapterCliClient")
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = listOf("-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager")
}