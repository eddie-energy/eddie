import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    id("energy.eddie.java-conventions")
    alias(libs.plugins.jsonschema2pojo)
}

group = "energy.eddie"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.jackson.databind)
    testImplementation(libs.junit.jupiter)
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
    isFormatDateTimes = true    // serialize ZonedDateTime to ISO 8601 string; should be named FormatDateTimes according to documentation...
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
    options.errorprone {
        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:AnnotatedPackages", "eddie.energy.regionconnector.api")

        // disable warnings for generated classes
        option("NullAway:TreatGeneratedAsUnannotated", true)
    }
}