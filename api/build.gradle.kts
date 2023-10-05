import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

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

dependencies {
    implementation(libs.microprofile.config)
    implementation(libs.jackson.databind)
    implementation(libs.javalin)
    testImplementation(libs.junit.jupiter)
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