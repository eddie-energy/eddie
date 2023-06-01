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
    testImplementation(libs.junit.jupiter)

    implementation(libs.jackson.databind)
    implementation("jakarta.validation:jakarta.validation-api:3.0.0")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}


jsonSchema2Pojo {
    // https://github.com/joelittlejohn/jsonschema2pojo/tree/master/jsonschema2pojo-gradle-plugin

    sourceFiles = listOf(projectDir.resolve("src/main/resources/schemas/"))
    targetDirectory = buildDir.resolve("generated-sources")
    targetPackage = "energy.eddie.regionconnector.api.v0.models"
    setAnnotationStyle("jackson2")
    dateTimeType = "java.time.ZonedDateTime"
    isFormatDateTimes = true    // serialize ZonedDateTime to ISO 8601 string
    generateBuilders = true
    includeGetters = true
    includeJsr305Annotations = false
    includeHashcodeAndEquals = false
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        check("NullAway", CheckSeverity.ERROR)
        option("NullAway:AnnotatedPackages", "energy.eddie.regionconnector.api")

        // disable warnings for generated classes
        option("NullAway:TreatGeneratedAsUnannotated", true)
    }
}