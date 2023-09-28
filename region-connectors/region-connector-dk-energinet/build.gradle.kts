import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

plugins {
    id("energy.eddie.java-conventions")
    application
    id("org.openapi.generator") version "6.6.0"
    id("org.gradlex.extra-java-module-info") version "1.3"
}

group = "energy.eddie"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(mapOf("path" to ":region-connectors:shared")))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)

    // Required for openapi generator
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.databind.nullable)
    implementation(libs.jakarta.annotation.api)

    // Required for the generated API client
    implementation(libs.feign.core)
    implementation(libs.feign.okhttp)
    implementation(libs.feign.jackson)
    implementation(libs.feign.slf4j)
    implementation(libs.feign.form)

    // sl4j
    implementation(libs.slf4j.api)
    implementation(libs.log4j.sl4j2.impl)
    runtimeOnly(libs.log4j.jul)


    implementation(libs.reactor.core)
    testImplementation(libs.reactor.test)
    implementation(libs.javalin)

    implementation(project(mapOf("path" to ":api")))
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
    automaticModule("commons-logging:commons-logging", "commons.logging")

    automaticModule("io.github.openfeign.form:feign-form", "feign.form")

    automaticModule("org.eclipse.microprofile.config:microprofile-config-api", "eclipse.microprofile.config.api")
}

tasks.test {
    useJUnitPlatform()
}

// Directory for generated java files
val generatedSwaggerJavaDir = "${buildDir}/generated/sources/swagger/main/java"

// Add generated sources to the main source set
sourceSets {
    main {
        java {
            srcDir(generatedSwaggerJavaDir)
        }
    }
    test {
        java {
            srcDir(generatedSwaggerJavaDir)
        }
    }
}

val packagePrefix = "energy.eddie.regionconnector.dk.energinet"
val customerApiPackagePrefix = "${packagePrefix}.customer"

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("${projectDir}/src/main/resources/energinet-customer-api-client-v3.json")
    outputDir.set(generatedSwaggerJavaDir)
    ignoreFileOverride.set("${projectDir}/src/main/resources/.openapi-generator-ignore")

    apiPackage.set("${customerApiPackagePrefix}.api")
    invokerPackage.set("${customerApiPackagePrefix}.invoker")
    modelPackage.set("${customerApiPackagePrefix}.model")

    generateApiTests.set(false)
    generateApiDocumentation.set(false)
    generateModelTests.set(false)
    generateModelDocumentation.set(false)
    configOptions.set(mapOf(
            "sourceFolder" to "/",
            "useJakartaEe" to "true",
            "dateLibrary" to "java8",
    ))

    library.set("feign")
    cleanupOutput.set(true)
}

tasks.named("compileJava").configure {
    dependsOn(tasks.named("openApiGenerate"))
}

tasks.withType<JavaCompile>().configureEach {
    if (!name.lowercase(Locale.getDefault()).contains("test") && !name.lowercase(Locale.getDefault()).contains("generated")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", packagePrefix)
            option("NullAway:UnannotatedClasses", "${customerApiPackagePrefix}.model")
            option("NullAway:TreatGeneratedAsUnannotated", true)
            // Regex fits to Windows and Unix-style path separators. CAVEAT: excludedPaths needs a regex string!
            val regexString = ".*/energy/eddie/regionconnector/dk/energinet/customer/invoker/.*".replace("/", "[/\\\\]")
            this.excludedPaths.set(regexString)
            option("NullawayExcludedClasses=EnerginetCliClient.java")
        }
    }
}

application {
    mainClass.set("${customerApiPackagePrefix}.EnerginetCustomerCliClient")
    applicationDefaultJvmArgs = listOf("-Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager")
}