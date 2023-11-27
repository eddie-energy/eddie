import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

plugins {
    application
    id("energy.eddie.java-conventions")
    alias(libs.plugins.jte.gradle)
}

group = "energy.eddie"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

jte {
    precompile() // use precompile instead of generate becouse errorprone would complain about generated code
}
tasks.jar {
    dependsOn(tasks.precompileJte)
    from(fileTree("jte-classes") {
        include("**/*.class")
    })
}

dependencies {
    implementation(libs.guice)
    implementation(libs.javalin)
    implementation(libs.javalin.rendering)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jdbi3.core)
    implementation(libs.reactor.core)
    implementation(libs.jte)
    implementation(libs.slf4j.simple)
    runtimeOnly(libs.h2database)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter)
}

application {
    mainClass.set("energy.eddie.examples.exampleapp.ExampleApp")
}

tasks.register("run-example-app", JavaExec::class) {
    mainClass.set(application.mainClass)
    classpath = sourceSets["main"].runtimeClasspath
    systemProperties.set("developmentMode", "true")
    group = "development"
    description = "run the example-app in development mode (for Jte templates)"
    environment["JDBC_URL"] = "jdbc:h2:tcp://localhost:9091/./examples/example-app"
    environment["PUBLIC_CONTEXT_PATH"] = ""
    environment["EDDIE_PUBLIC_URL"] = "http://localhost:8080"
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    if (!name.lowercase(Locale.getDefault()).contains("test")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "energy.eddie")
            option("NullAway:ExcludedFieldAnnotations", "com.google.inject.Inject")
        }
    }
}