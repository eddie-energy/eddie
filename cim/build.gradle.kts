import org.w3c.dom.Element
import org.w3c.dom.NodeList
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path

plugins {
    id("java")
    `maven-publish`
    jacoco
}

group = "energy.eddie"
version = "3.2.0"

repositories {
    mavenCentral()
}

// JAXB configuration holds classpath for running the JAXB XJC compiler
val jaxb: Configuration by configurations.creating

dependencies {
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.datatype.jdk8)
    implementation(libs.jackson.dataformat.xml)
    implementation(libs.jackson.jakarta.xmlbind.annotations)
    implementation(libs.jaxb.runtime)
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
    // ordered schema files to prevent repeated generation of Java classes.
    val orderedSchemaFiles = setOf(
        // V0.82
        File(cimSchemaFiles, "/v0_82/vhd/ValidatedHistoricalData_MarketDocument_2024-06-21T12.10.53.xsd"),
        File(cimSchemaFiles, "/v0_82/ap/AccountingPoint_MarketDocument_2024-06-21T11.38.58.xsd"),
        File(cimSchemaFiles, "/v0_82/pmd/Permission_Envelope_2024-06-21T11.51.02.xsd"),
        // V0.91.08
        File(cimSchemaFiles, "/v0_91_08/RedistributionTransactionRequest Document_Annotated.xsd"),
        // V1.04
        File(cimSchemaFiles, "/v1_04/vhd/ValidatedHistoricalData Document_v1.04_annotated.xsd"),
        File(cimSchemaFiles, "/v1_04/rtd/RealTimeData Document_v1.04_Annotated.xsd"),
        File(cimSchemaFiles, "/v1_04/pmd/Permission Document_v1.04_annotated.xsd"),
        File(cimSchemaFiles, "/v1_04/ap/AccountingPointData Document_v1.04_annotated.xsd"),
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
        for (srcFile in cimSchemaFiles.walkTopDown()) {
            if (!srcFile.isFile || srcFile.extension != xsdExtension) {
                continue
            }
            val xjbFileBasename = srcFile.name.dropLast(xsdExtension.length) + "xjb"
            // Create a copy of the source file to not accidentally manipulate the real file
            val tmpSrcFile = srcFile.copyTo(temporaryDir.resolve(srcFile.relativeTo(cimSchemaFiles)), true)
            val xjbFile = temporaryDir.resolve(
                srcFile.parentFile.resolve(xjbFileBasename).relativeTo(cimSchemaFiles)
            )
            xsdToGenerate.add(Triple(srcFile, tmpSrcFile, xjbFile))
        }
        for (files in xsdToGenerate) {
            val srcFile = files.first
            val tmpSrcFile = files.second
            val xjbFile = files.third
            if (!orderedSchemaFiles.contains(srcFile)) {
                continue
            }
            // generate the bindings file
            logger.log(LogLevel.INFO, "Generating bindings for ${tmpSrcFile.name}")
            generateBindingsFile(tmpSrcFile, xjbFile.absolutePath)
            logger.log(LogLevel.LIFECYCLE, "Generating for ${tmpSrcFile.name}")
            val execution = providers.exec {
                executable(Path(System.getProperty("java.home"), "bin", "java"))
                val classpath = jaxb.resolve().joinToString(File.pathSeparator)
                args(
                    "-cp", classpath, "com.sun.tools.xjc.XJCFacade",
                    "-d", generatedXJCJavaDir,
                    tmpSrcFile.absolutePath,
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
        create<MavenPublication>("cim") {
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
                from(components["java"])
            }
        }
    }
}

fun generateBindingsFile(rootXsd: File, bindingsFilePath: String) {
    val documentBuilderFactory = DocumentBuilderFactory
        .newInstance()
    documentBuilderFactory.isNamespaceAware = true
    // Get all referenced XSDs
    val xsdNs = "http://www.w3.org/2001/XMLSchema"
    val nextXsds = ArrayDeque(listOf(rootXsd))
    val visitedXsds = HashSet<File>()
    logger.lifecycle("Finding all related XSDs to {}", rootXsd)
    while (nextXsds.isNotEmpty()) {
        val currentXsd = nextXsds.removeFirst()
        logger.info("Continuing with {}", currentXsd)
        if (visitedXsds.contains(currentXsd)) {
            logger.lifecycle("Discarding {} as it already has been scanned for imports", currentXsd)
            continue
        }
        logger.lifecycle("Scanning {}", currentXsd)
        val xsdDocument = documentBuilderFactory
            .newDocumentBuilder()
            .parse(currentXsd)
        val imports = xsdDocument.getElementsByTagNameNS(xsdNs, "import")
        val includes = xsdDocument.getElementsByTagNameNS(xsdNs, "include")
        nextXsds.addAll(getAllXsdReferences(imports, currentXsd))
        nextXsds.addAll(getAllXsdReferences(includes, currentXsd))
        visitedXsds.add(currentXsd)
    }
    logger.lifecycle("For {} found these XSDs: {}", rootXsd, visitedXsds)
    val bindings = StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        .append("<bindings xmlns=\"https://jakarta.ee/xml/ns/jaxb\" ")
        .append("xmlns:annox=\"http://annox.dev.java.net\" xmlns:jaxb=\"https://jakarta.ee/xml/ns/jaxb\" jaxb:extensionBindingPrefixes=\"xjc annox\" ")
        .append("xmlns:xs=\"http://www.w3.org/2001/XMLSchema\" ")
        .append("xmlns:xjc=\"http://java.sun.com/xml/ns/jaxb/xjc\" ")
        .append("version=\"3.0\">\n")
        .append("    <globalBindings typesafeEnumMaxMembers=\"2000\">\n") // There is an arbitrary limit of 256 enumeration types per enumeration
        .append("           <xjc:simple/>\n") // Removes ComplexType Suffix from Root Elements
        .append("           <xjc:javaType name=\"java.time.ZonedDateTime\"\n")
        .append("                         xmlType=\"xs:dateTime\"\n")
        .append("                         adapter=\"energy.eddie.cim.v1_04.extensions.CimDateTimeAdapter\"/>\n")
        .append("    </globalBindings>\n")
    for (file in visitedXsds) {
        bindings.append(generateBindingsForReferencedFile(file))
    }
    bindings
        .append("</bindings>\n")
    File(bindingsFilePath).writeText(bindings.toString())
}

fun getAllXsdReferences(nodes: NodeList, relative: File): Set<File> {
    val founds = HashSet<File>()
    for (i in 0 until nodes.length) {
        val element = nodes.item(i) as Element
        val ref = element.getAttribute("schemaLocation")
        if (ref.isNullOrBlank()) {
            continue
        }
        val found = relative.resolveSibling(ref).canonicalFile
        logger.lifecycle("Found {}", found)
        if (!found.isFile) {
            logger.lifecycle("Not a file {}", found)
        }
        founds.add(found)
    }
    return founds
}

// Generate a bindings file that customizes the generated code, so that the enums are usable and the root elements don't have the ComplexType suffix
fun generateBindingsForReferencedFile(xsdFile: File): String {
    val documentBuilderFactory = DocumentBuilderFactory
        .newInstance()
    documentBuilderFactory.isNamespaceAware = true
    val xsdDocument = documentBuilderFactory
        .newDocumentBuilder()
        .parse(xsdFile)
    val schemaLocation = xsdFile.absolutePath

    val xmlSchemaNs = "http://www.w3.org/2001/XMLSchema"
    val packageName = if (xsdFile.parentFile.parentFile.name.equals("generateCIMSchemaClasses")) {
        "energy.eddie.cim." + xsdFile.parentFile.name
    } else {
        "energy.eddie.cim." + xsdFile.parentFile.parentFile.name + "." + xsdFile.parentFile.name
    }
    logger.info("Package name for {} is {}", xsdFile, packageName)
    val bindings = StringBuilder()
        .append("    <bindings schemaLocation=\"${schemaLocation}\" node=\"/xs:schema\">\n")
        .append("           <schemaBindings> <package name=\"${packageName}\"/> </schemaBindings>\n")

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
    return bindings.toString()
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
