import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path

plugins {
    id("java")
    `maven-publish`
    jacoco
}

group = "energy.eddie"
version = "2.0.1"

repositories {
    mavenCentral()
}

// JAXB configuration holds classpath for running the JAXB XJC compiler
val jaxb: Configuration by configurations.creating

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.jackson.annotations)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.jakarta.validation.api)
    implementation(libs.jakarta.xml.bind.api)
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // dependencies needed to generate code
    jaxb(libs.jaxb.xjc)
    jaxb(libs.jaxb.runtime)
    jaxb(libs.jaxb.plugins)
    jaxb(libs.jaxb.plugin.annotate)
    jaxb(libs.jackson.annotations)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.withType<JacocoReport>())
}

tasks.withType<Test>().configureEach {
}

tasks.withType<JacocoReport> {
    reports {
        xml.required.set(true)
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
        resources {
            srcDir("${projectDir}/src/main/schemas")
        }
    }
    test {
        java {
            srcDir(generatedXJCJavaDir)
        }
        resources {
            srcDir("${projectDir}/src/main/schemas")
        }
    }
}

@OptIn(ExperimentalPathApi::class)
val generateCIMSchemaClasses = tasks.register("generateCIMSchemaClasses") {
    description = "Generate CIM Java Classes from XSD files"
    group = "Build"

    // Path to XSD files
    val cimSchemaFiles = file("src/main/schemas/cim/xsd/")
    // ordered schema files to prevent generation of Java classes without the correct bindings.
    // Top-down order, first files are the ones are the ones that are not imported by any other XSD and import the others.
    // Then, the ones that are imported by others and import other XSDs.
    // Lastly, the ones that do not import any other file and are only imported by others.
    val orderedSchemaFiles = listOf(
        // V0.82
        File(cimSchemaFiles, "/v0_82/vhd/ValidatedHistoricalData_MarketDocument_2024-06-21T12.10.53.xsd"),
        File(cimSchemaFiles, "/v0_82/ap/AccountingPoint_MarketDocument_2024-06-21T11.38.58.xsd"),
        File(cimSchemaFiles, "/v0_82/pmd/Permission_Envelope_2024-06-21T11.51.02.xsd"),
        File(cimSchemaFiles, "/v0_82/pmd/Permission_Envelope_2024-06-21T11.51.02.xsd"),
        // V0.92.08
        File(cimSchemaFiles, "/v0_91_08/RedistributionTransactionRequest Document_Annotated.xsd"),
        File(cimSchemaFiles, "/v0_91_08/ValidateHistoricalData Document_Annotated.xsd"),
        File(cimSchemaFiles, "/v0_91_08/urn-entsoe-eu-wgedi-components.xsd"),
        File(cimSchemaFiles, "/v0_91_08/urn-entsoe-eu-local-extension-types.xsd"),
        File(cimSchemaFiles, "/v0_91_08/urn-entsoe-eu-wgedi-codelists.xsd"),
        // V1.04: Near Real Time Data
        File(cimSchemaFiles, "/v1_04/RealTimeData Document_v1.04.xsd"),
        File(cimSchemaFiles, "/v1_04/RealTimeData Document_v1.04_Annotated.xsd"),
        File(cimSchemaFiles, "/v1_04/urn-entsoe-eu-wgedi-components.xsd"),
        File(cimSchemaFiles, "/v1_04/urn-entsoe-eu-local-extension-types.xsd"),
        File(cimSchemaFiles, "/v1_04/urn-entsoe-eu-wgedi-codelists.xsd"),
    )

    // Define the task inputs and outputs, so Gradle can track changes and only run the task when needed
    inputs.files(fileTree(cimSchemaFiles).include("**/*.xsd"))
    outputs.dir(generatedXJCJavaDir)

    val xsdExtension = "xsd"
    doLast {
        // make sure the directory exists
        file(generatedXJCJavaDir).mkdirs()
        val xsdToGenerate = ArrayList<Triple<File, File, File>>()
        // Copy all files first, so they exist in the target directory
        for (srcFile in orderedSchemaFiles) {
            if (!srcFile.isFile || srcFile.extension != xsdExtension) {
                continue
            }
            val xjbFileBasename = srcFile.name.dropLast(xsdExtension.length) + "xjb"
            // Create a copy of the source file to not accidentally manipulate the real file
            val tmpSrcFile = srcFile.copyTo(temporaryDir.resolve(srcFile.relativeTo(cimSchemaFiles)), true)
            val xjbFile = temporaryDir.resolve(
                srcFile.parentFile.resolve(xjbFileBasename).relativeTo(cimSchemaFiles)
            )
            // generate the bindings file
            logger.log(LogLevel.INFO, "Generating bindings for ${srcFile.name}")
            generateBindingsFile(srcFile, xjbFile.absolutePath)
            xsdToGenerate.add(Triple(srcFile, tmpSrcFile, xjbFile))
        }
        xsdToGenerate.forEach { files: Triple<File, File, File> ->
            val srcFile = files.first
            val tmpSrcFile = files.second
            val xjbFile = files.third
            val packageName = if (srcFile.parentFile.parentFile.name.equals("xsd")) {
                "energy.eddie.cim." + srcFile.parentFile.name
            } else {
                "energy.eddie.cim." + srcFile.parentFile.parentFile.name + "." + srcFile.parentFile.name
            }
            logger.log(LogLevel.LIFECYCLE, "Generating for ${tmpSrcFile.name}")
            val execution = providers.exec {
                executable(Path(System.getProperty("java.home"), "bin", "java"))
                val classpath = jaxb.resolve().joinToString(File.pathSeparator)
                args(
                    "-cp", classpath, "com.sun.tools.xjc.XJCFacade",
                    "-d", generatedXJCJavaDir,
                    tmpSrcFile.absolutePath,
                    "-p", packageName,
                    "-b", xjbFile.absolutePath,
                    "-mark-generated", "-npa", "-encoding", "UTF-8",
                    "-extension", "-Xfluent-api", "-Xannotate"
                )
            }
            val res = execution.result.get()
            val stdOut = execution.standardOutput.asText
            if (stdOut.isPresent) {
                logger.log(LogLevel.LIFECYCLE, stdOut.get())
            }
            if (res.exitValue != 0) {
                val stdError = execution.standardError.asText
                if (stdError.isPresent) {
                    logger.log(LogLevel.WARN, stdError.get())
                }
            }
        }
    }
}

tasks.named("compileJava") {
    // generate the classes before compiling
    dependsOn(generateCIMSchemaClasses)
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/eddie-energy/eddie")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GPR_USER")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("GPR_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            pom {
                name = "cim"
                artifactId = "cim"
                version = project.version.toString()
                description = "Generated CIM classes"
                url = "https://github.com/eddie-energy/eddie"
                developers {
                    developer {
                        id = "eddie-energy"
                        name = "EDDIE Energy"
                        email = "developers@eddie.energy"
                    }
                }
                scm {
                    url = "https://github.com/eddie-energy/eddie"
                }
            }
        }
    }
}

// Generate a bindings file that customizes the generated code, so that the enums are usable and the root elements don't have the ComplexType suffix
fun generateBindingsFile(xsdFile: File, bindingsFilePath: String) {
    val documentBuilderFactory = DocumentBuilderFactory
        .newInstance()
    documentBuilderFactory.isNamespaceAware = true
    val xsdDocument = documentBuilderFactory
        .newDocumentBuilder()
        .parse(xsdFile)
    val schemaLocation = xsdFile.name

    val xmlSchemaNs = "http://www.w3.org/2001/XMLSchema"
    val bindings = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    bindings
        .append("<bindings xmlns=\"https://jakarta.ee/xml/ns/jaxb\" ")
        .append("xmlns:annox=\"http://annox.dev.java.net\" xmlns:jaxb=\"https://jakarta.ee/xml/ns/jaxb\" jaxb:extensionBindingPrefixes=\"xjc annox\" ")
        .append("xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" ")
        .append("xmlns:xjc=\"http://java.sun.com/xml/ns/jaxb/xjc\" ")
        .append("version=\"3.0\">\n")
        .append("    <globalBindings typesafeEnumMaxMembers=\"2000\">\n") // There is an arbitrary limit of 256 enumeration types per enumeration
        .append("           <xjc:simple/>\n") // Removes ComplexType Suffix from Root Elements
        .append("           <xjc:javaType name=\"java.time.ZonedDateTime\"\n")
        .append("                         xmlType=\"xs:dateTime\"\n")
        .append("                         adapter=\"energy.eddie.cim.v0_91_08.extensions.CimDateTimeAdapter\"/>\n")
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

    val simpleTypes = xsdDocument.getElementsByTagNameNS(xmlSchemaNs, "simpleType")
    // Make enums use the documentation as the enum name, as the values are not readable / incompatible with Java
    for (i in 0 until simpleTypes.length) {
        val simpleType = simpleTypes.item(i) as Element
        val enumerationElements = simpleType.getElementsByTagNameNS(xmlSchemaNs, "enumeration")
        if (enumerationElements.length == 0) continue
        val typeName = simpleType.getAttribute("name")
        bindings.append("        <bindings node=\"//xs:simpleType[@name='$typeName']\">\n")
        bindings.append("            <annox:annotateEnumValueMethod>@com.fasterxml.jackson.annotation.JsonValue</annox:annotateEnumValueMethod>\n")
        bindings.append("            <annox:annotateEnumFromValueMethod>@com.fasterxml.jackson.annotation.JsonCreator</annox:annotateEnumFromValueMethod>\n")
        bindings.append("            <typesafeEnumClass name=\"${typeName.toJavaClassName()}\">\n")

        for (j in 0 until enumerationElements.length) {
            val enumElement = enumerationElements.item(j) as Element
            val value = enumElement.getAttribute("value")
            val title = enumElement.getElementsByTagName("Title")
            val javaName = if (title.length > 0) {
                title.item(0).textContent.trimEnd().toJavaEnumName()
            } else {
                val doc = enumElement.getElementsByTagNameNS(xmlSchemaNs, "documentation")
                if (doc.length > 0) { // only use the documentation if it exists
                    doc.item(0).textContent.trimEnd().toJavaEnumName()
                } else value.toJavaEnumName()
            }
            // Makes the generated enums have usable names
            bindings.append("                <typesafeEnumMember name=\"$javaName\" value=\"$value\"/>\n")
        }

        bindings.append("            </typesafeEnumClass>\n")
        bindings.append("        </bindings>\n")
    }

    bindings.append("    </bindings>\n")
    bindings.append("</bindings>\n")

    File(bindingsFilePath).writeText(bindings.toString())
}

fun String.toJavaEnumName(): String {
    val name = this.split("\\s+".toRegex())
        .joinToString("_") { it.uppercase() }
        .replace("[^A-Za-z0-9_]".toRegex(), "")
    if (Character.isDigit(name[0])) {
        return "_$name"
    }
    return name
}

fun String.toJavaClassName(): String =
    this.split("\\s+".toRegex())
        .joinToString("") { it.replaceFirstChar(Char::uppercase) }
        .replace("[^A-Za-z0-9]".toRegex(), "")
