import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.w3c.dom.Element
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.Path

plugins {
    id("energy.eddie.java-conventions")
    alias(libs.plugins.jsonschema2pojo)
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
    implementation(libs.jackson.annotations)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.reactor.core)

    testImplementation(libs.junit.jupiter)

    // dependencies needed to generate code
    jaxb(libs.jaxb.xjc)
    jaxb(libs.jaxb.runtime)
    jaxb(libs.jaxb.plugins)
    jaxb(libs.jaxb.plugin.annotate)
    jaxb(libs.jackson.annotations)


}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}


jsonSchema2Pojo {
    // https://github.com/joelittlejohn/jsonschema2pojo/tree/master/jsonschema2pojo-gradle-plugin
    sourceFiles = listOf(projectDir.resolve("src/main/schema.json"))
    targetDirectory = project.layout.buildDirectory.asFile.get().resolve("generated-sources")
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
val generatedXJCJavaDir = "${project.layout.buildDirectory.asFile.get()}/generated/sources/xjc/main/java"

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

val generateCIMSchemaClasses = tasks.register("generateCIMSchemaClasses") {
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
            if (!srcFile.isFile || srcFile.extension != xsdExtension) {
                return@forEach
            }
            val xjbFileBasename = srcFile.name.dropLast(xsdExtension.length) + "xjb"
            val file = srcFile.copyTo(temporaryDir.resolve(srcFile.relativeTo(cimSchemaFiles)), true)
            val xjbFile = temporaryDir.resolve(
                srcFile.parentFile.resolve(xjbFileBasename).relativeTo(cimSchemaFiles)
            ) // generate the bindings file
            generateBindingsFile(srcFile, xjbFile.absolutePath)

            val packageName =
                "energy.eddie.cim." + srcFile.parentFile.parentFile.name + "." + srcFile.parentFile.name
            exec {
                executable(Path(System.getProperty("java.home"), "bin", "java"))
                val classpath = jaxb.resolve().joinToString(File.pathSeparator)
                args(
                    "-cp", classpath, "com.sun.tools.xjc.XJCFacade",
                    "-d", generatedXJCJavaDir,
                    file.absolutePath,
                    "-p", packageName,
                    "-b", xjbFile.absolutePath,
                    "-mark-generated", "-npa", "-encoding", "UTF-8",
                    "-extension", "-Xfluent-api", "-Xannotate"
                )
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
    bindings
        .append("<bindings xmlns=\"https://jakarta.ee/xml/ns/jaxb\" ")
        .append("xmlns:annox=\"http://annox.dev.java.net\" xmlns:jaxb=\"https://jakarta.ee/xml/ns/jaxb\" jaxb:extensionBindingPrefixes=\"xjc annox\" ")
        .append("xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" ")
        .append("xmlns:xjc=\"http://java.sun.com/xml/ns/jaxb/xjc\" ")
        .append("version=\"3.0\">\n")
        .append("    <globalBindings>\n")
        .append("           <xjc:simple/>\n") // Removes ComplexType Suffix from Root Elements
        .append("    </globalBindings>\n")
        .append("    <bindings schemaLocation=\"${schemaLocation}\" node=\"/xs:schema\">\n")

    if (xsdDocument.getElementsByTagName("DateAndOrTimeComplexType").length > 0) {
        // Add annotations to the date and time fields of the DateAndOrTimeComplexType
        bindings
            .append("        <bindings node=\"//xs:complexType[@name='DateAndOrTimeComplexType']/xs:sequence/xs:element[@name='date']\">\n")
            .append("            <annox:annotate target=\"field\">@com.fasterxml.jackson.annotation.JsonFormat(shape = JsonFormat.Shape.STRING, pattern = \"yyyy-MM-dd\")</annox:annotate>\n")
            .append("        </bindings>\n")
            .append("        <bindings node=\"//xs:complexType[@name='DateAndOrTimeComplexType']/xs:sequence/xs:element[@name='time']\">\n")
            .append("            <annox:annotate target=\"field\">@com.fasterxml.jackson.annotation.JsonFormat(shape = JsonFormat.Shape.STRING, pattern = \"HH:mm:ss.SSS'Z'\")</annox:annotate>\n")
            .append("        </bindings>\n")
    }
    // Make enums use the documentation as the enum name, as the values are not readable / incompatible with Java
    for (i in 0 until simpleTypes.length) {
        val simpleType = simpleTypes.item(i) as Element
        val enumerationElements = simpleType.getElementsByTagName("xs:enumeration")
        if (enumerationElements.length > 0) {
            val typeName = simpleType.getAttribute("name")
            bindings.append("        <bindings node=\"//xs:simpleType[@name='$typeName']\">\n")
            bindings.append("            <annox:annotateEnumValueMethod>@com.fasterxml.jackson.annotation.JsonValue</annox:annotateEnumValueMethod>\n")
            bindings.append("            <annox:annotateEnumFromValueMethod>@com.fasterxml.jackson.annotation.JsonCreator</annox:annotateEnumFromValueMethod>\n")
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