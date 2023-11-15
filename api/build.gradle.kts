import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.w3c.dom.Element
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.Path

plugins {
    id("energy.eddie.java-conventions")
    alias(libs.plugins.jsonschema2pojo)
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
    implementation(libs.microprofile.config)
    implementation(libs.jackson.databind)
    testImplementation(libs.junit.jupiter)
    // dependencies needed to generate code
    jaxb(libs.jaxb.xjc)
    jaxb(libs.jaxb.runtime)
    // https://mvnrepository.com/artifact/org.jvnet.jaxb2_commons/jaxb2-fluent-api
    jaxb("org.jvnet.jaxb2_commons:jaxb2-fluent-api:3.0")

    implementation(libs.jakarta.xml.bind.api)
    implementation(libs.jakarta.annotation.api)
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


jsonSchema2Pojo {
    // https://github.com/joelittlejohn/jsonschema2pojo/tree/master/jsonschema2pojo-gradle-plugin
    sourceFiles = listOf(projectDir.resolve("src/main/schema.json"))
    targetDirectory = buildDir.resolve("generated-sources")
    targetPackage = "energy.eddie.api.v0"
    setAnnotationStyle("jackson2")
    dateTimeType = "java.time.ZonedDateTime"
    isFormatDateTimes = true    // serialize ZonedDateTime to ISO 8601 string
    generateBuilders = true
    includeGetters = true
    includeJsr305Annotations = false
    includeHashcodeAndEquals = false
}

sourceSets {
    create("schema") {
        java {
            srcDir(projectDir.resolve("src/main/schema.json"))
        }
    }
}

tasks.withType<JavaCompile>().configureEach {
    if (!name.lowercase(Locale.getDefault()).contains("test")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "energy.eddie")

            // disable warnings for generated classes
            option("NullAway:TreatGeneratedAsUnannotated", true)
        }
    }
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

val generateCIMSchemaClasses = tasks.create("generateCIMSchemaClasses") {
    description = "Generate CIM Java Classes from XSD files"
    group = "Build"

    // Path to XSD files
    val cimSchemaFiles = file("src/main/schemas/cim/xsd/")

    // Define the task inputs and outputs, so Gradle can track changes and only run the task when needed
    inputs.files(fileTree(cimSchemaFiles).include("**/*.xsd"))
    outputs.dir(generatedXJCJavaDir)

    val xsdExtension = "xsd"
    doLast {
        // make sure the directory exists
        file(generatedXJCJavaDir).mkdirs()

        // iterate each folder and generate the classes with the same package name
        cimSchemaFiles.walk().forEach { srcFile ->
            if (srcFile.isFile && srcFile.extension == xsdExtension) {
                val xjbFileBasename = srcFile.name.dropLast(xsdExtension.length) + "xjb"
                val file = srcFile.copyTo(temporaryDir.resolve(srcFile.relativeTo(cimSchemaFiles)), true)
                val xjbFile = temporaryDir.resolve(srcFile.parentFile.resolve(xjbFileBasename).relativeTo(cimSchemaFiles)) // generate the bindings file
                generateBindingsFile(srcFile, xjbFile.absolutePath)

                val packageName = "energy.eddie.cim." + srcFile.parentFile.parentFile.name + "." + srcFile.parentFile.name
                exec {
                    executable(Path(System.getProperty("java.home"), "bin", "java"))
                    val classpath = jaxb.resolve().joinToString(File.pathSeparator)
                    args("-cp", classpath, "com.sun.tools.xjc.XJCFacade", "-d", generatedXJCJavaDir, file.absolutePath, "-p", packageName, "-b", xjbFile.absolutePath, "-mark-generated", "-npa", "-extension", "-Xfluent-api")
                }
            }
        }
    }
}

tasks.named("compileJava") {
    // generate the classes before compiling
    dependsOn(generateCIMSchemaClasses)
}

// Generate a bindings file that customizes the generated code, so that the enums are usable and the root elements don't have the ComplexType suffix
fun generateBindingsFile(xsdFile: File, bindingsFilePath: String) {
    val docBuilderFactory = DocumentBuilderFactory.newInstance()
    val docBuilder = docBuilderFactory.newDocumentBuilder()
    val xsdDocument = docBuilder.parse(xsdFile)
    val schemaLocation = xsdFile.name

    val simpleTypes = xsdDocument.getElementsByTagName("xs:simpleType")
    val bindings = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    bindings.append("<bindings xmlns=\"https://jakarta.ee/xml/ns/jaxb\" ")
            .append("xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" ")
            .append("xmlns:xjc=\"http://java.sun.com/xml/ns/jaxb/xjc\" ")
            .append("version=\"3.0\">\n")
    bindings.append("    <globalBindings>\n")
    bindings.append("           <xjc:simple/>\n") // Removes ComplexType Suffix from Root Elements
    bindings.append("    </globalBindings>\n")
    bindings.append("    <bindings schemaLocation=\"${schemaLocation}\" node=\"/xs:schema\">\n")

    for (i in 0 until simpleTypes.length) {
        val simpleType = simpleTypes.item(i) as Element
        val enumerationElements = simpleType.getElementsByTagName("xs:enumeration")
        if (enumerationElements.length > 0) {
            val typeName = simpleType.getAttribute("name")
            bindings.append("        <bindings node=\"//xs:simpleType[@name='$typeName']\">\n")
            bindings.append("            <typesafeEnumClass name=\"${typeName.toJavaClassName()}\">\n")

            for (j in 0 until enumerationElements.length) {
                val enumElement = enumerationElements.item(j) as Element
                val value = enumElement.getAttribute("value")
                val doc = enumElement.getElementsByTagName("xs:documentation").item(0).textContent.trimEnd()
                val javaName = doc.toJavaEnumName()
                // Makes the generated enums have usable names
                bindings.append("                <typesafeEnumMember name=\"$javaName\" value=\"$value\"/>\n")
            }

            bindings.append("            </typesafeEnumClass>\n")
            bindings.append("        </bindings>\n")
        }
    }

    bindings.append("    </bindings>\n")
    bindings.append("</bindings>\n")

    File(bindingsFilePath).writeText(bindings.toString())
}


fun String.toJavaEnumName(): String =
        this.split("\\s+".toRegex())
                .joinToString("_") { it.uppercase() }
                .replace("[^A-Za-z0-9_]".toRegex(), "")

fun String.toJavaClassName(): String =
        this.split("\\s+".toRegex())
                .joinToString("") { it.replaceFirstChar(Char::uppercase) }
                .replace("[^A-Za-z0-9]".toRegex(), "")