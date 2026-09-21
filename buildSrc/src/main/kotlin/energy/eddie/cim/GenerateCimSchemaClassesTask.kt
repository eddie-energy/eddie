// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie.cim

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.collections.ArrayDeque

/**
 * Generates CIM Java classes from the configured XSD entry point schemas by invoking the JAXB XJC compiler.
 */
abstract class GenerateCimSchemaClassesTask : DefaultTask() {

    /** Directory containing all CIM XSD schemas. */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val schemaDirectory: DirectoryProperty

    /** Schema files (relative to [schemaDirectory]) used as entry points for the code generation. */
    @get:Input
    abstract val entryPointSchemas: ListProperty<String>

    /** Classpath used to run the JAXB XJC compiler. */
    @get:Classpath
    abstract val jaxbClasspath: ConfigurableFileCollection

    /** Directory the generated Java sources are written to. */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun generate() {
        val schemaDir = schemaDirectory.get().asFile
        val outputDir = outputDirectory.get().asFile
        val workDir = temporaryDir

        // Create a copy of the source files to not accidentally manipulate the real files
        fileSystemOperations.copy {
            from(schemaDir)
            into(workDir)
        }

        val entryPoints = entryPointSchemas.get().map { File(schemaDir, it) }.toSet()

        val xsdToGenerate = ArrayList<Triple<File, File, File>>()
        for (srcFile in schemaDir.walkTopDown()) {
            if (!srcFile.isFile || srcFile.extension != "xsd") {
                continue
            }
            val xjbFileBasename = srcFile.nameWithoutExtension + ".xjb"

            val tmpSrcFile = File(workDir, srcFile.relativeTo(schemaDir).path)
            val xjbFile = File(workDir, srcFile.parentFile.resolve(xjbFileBasename).relativeTo(schemaDir).path)
            xsdToGenerate.add(Triple(srcFile, tmpSrcFile, xjbFile))
        }
        for (files in xsdToGenerate) {
            val srcFile = files.first
            val tmpSrcFile = files.second
            val xjbFile = files.third
            if (!entryPoints.contains(srcFile)) {
                continue
            }
            // generate the bindings file
            logger.log(LogLevel.INFO, "Generating bindings for ${tmpSrcFile.name}")
            generateBindingsFile(tmpSrcFile, xjbFile.absolutePath)
            logger.log(LogLevel.LIFECYCLE, "Generating for ${tmpSrcFile.name}")
            val stdOut = ByteArrayOutputStream()
            val stdErr = ByteArrayOutputStream()
            val res = execOperations.exec {
                executable(File(File(System.getProperty("java.home"), "bin"), "java"))
                args(
                    "-cp", jaxbClasspath.asPath, "com.sun.tools.xjc.XJCFacade",
                    "-d", outputDir.absolutePath,
                    tmpSrcFile.absolutePath,
                    "-b", xjbFile.absolutePath,
                    "-mark-generated", "-npa", "-encoding", "UTF-8",
                    "-extension", "-Xfluent-api", "-Xannotate"
                )
                isIgnoreExitValue = true
                standardOutput = stdOut
                errorOutput = stdErr
            }
            if (stdOut.size() > 0) {
                logger.log(LogLevel.LIFECYCLE, stdOut.toString(Charsets.UTF_8))
            }
            if (res.exitValue != 0) {
                logger.error("Error while generating classes for ${tmpSrcFile.name}")
                if (stdErr.size() > 0) {
                    logger.error(stdErr.toString(Charsets.UTF_8))
                }
                throw GradleException("Could not generate classes for $srcFile")
            }
        }
    }

    private fun generateBindingsFile(rootXsd: File, bindingsFilePath: String) {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
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
            .append("           <xjc:javaType name=\"java.time.ZonedDateTime\"\n")
            .append("                         xmlType=\"xs:date\"\n")
            .append("                         adapter=\"energy.eddie.cim.extensions.CimDateAdapter\"/>\n")
            .append("    </globalBindings>\n")
        for (file in visitedXsds) {
            bindings.append(generateBindingsForReferencedFile(file))
        }
        bindings
            .append("</bindings>\n")
        File(bindingsFilePath).writeText(bindings.toString())
    }

    private fun getAllXsdReferences(nodes: NodeList, relative: File): Set<File> {
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
    private fun generateBindingsForReferencedFile(xsdFile: File): String {
        val documentBuilderFactory = DocumentBuilderFactory
            .newInstance()
        documentBuilderFactory.isNamespaceAware = true
        val xsdDocument = documentBuilderFactory
            .newDocumentBuilder()
            .parse(xsdFile)
        var schemaLocation = xsdFile.absolutePath

        if (System.getProperty("os.name").lowercase().contains("win")) {
            schemaLocation = xsdFile.toURI().toString()

            schemaLocation.prependIndent("file:/")
            schemaLocation.replace("\\", "/")
        }

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

    private fun String.toJavaEnumName(): String {
        val name = this.split("\\s+".toRegex())
            .joinToString("_") { it.uppercase() }
            .replace("[^A-Za-z0-9_]".toRegex(), "")
        if (Character.isDigit(name[0])) {
            return "_$name"
        }
        return name
    }

    private fun String.toJavaClassName(): String =
        this.split("\\s+".toRegex())
            .joinToString("") { it.replaceFirstChar(Char::uppercase) }
            .replace("[^A-Za-z0-9]".toRegex(), "")
}
