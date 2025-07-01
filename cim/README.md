# Information on adding a new CIM schema

## Look for imports

1. If there are imports inside the xsd file (e.g.
   `<xs:import schemaLocation="" namespace="urn:entsoe.eu:wgedi:codelists" />`), copy the necessary files inside the same directory.
    1. look for `urn-entse-eu-*.xsd` files
2. Inside the import brackets, enter the schema location for the imported file.
    1. e.g.
       `<xs:import schemaLocation="./urn-entsoe-eu-wgedi-codelists.xsd" namespace="urn:entsoe.eu:wgedi:codelists" />`

## Add the files to the build script

1. Add the new xsd files as well as the files needed for the imports to the build script.

```kotlin
// V1.04: Near Real Time Data
File(cimSchemaFiles, "/v1_04/RealTimeData Document_v1.04.xsd"),
File(cimSchemaFiles, "/v1_04/RealTimeData Document_v1.04_Annotated.xsd"),
File(cimSchemaFiles, "/v1_04/urn-entsoe-eu-wgedi-components.xsd"),
File(cimSchemaFiles, "/v1_04/urn-entsoe-eu-local-extension-types.xsd"),
File(cimSchemaFiles, "/v1_04/urn-entsoe-eu-wgedi-codelists.xsd"),
```

## Add the version to the namespace

1. Add the version numbers to `namespace` and
   `targetNamespace` attributes in the xsd files which reference the files copied before.
   Separate the version number from the namespace with a colon, being the last part of the namespace.
   (e.g.
   `<xs:import schemaLocation="./urn-entsoe-eu-wgedi-codelists.xsd" namespace="urn:entsoe.eu:wgedi:codelists:1.04" />`)

2. Add the version number to the `xmlns:ecl` attribute in the xsd files, when available
    1. e.g. `xmlns:ecl="urn:entsoe.eu:wgedi:codelists:1.04`

## Still not working?

Receive better error messages by temporarily editing the `build.gradle.kts` file by updating the following code:

```kotlin
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
```

Update this to the following:

```kotlin
exec {
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
```