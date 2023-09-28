import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

plugins {
    application
    id("energy.eddie.java-conventions")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "energy.eddie"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))
    implementation(project(mapOf("path" to ":outbound-kafka")))
    implementation(project(":region-connectors:region-connector-at-eda"))
    implementation(project(":region-connectors:region-connector-fr-enedis"))
    implementation(project(":region-connectors:region-connector-es-datadis"))
    implementation(project(":region-connectors:region-connector-simulation"))

    implementation(libs.microprofile.config)
    implementation("io.smallrye.config:smallrye-config:3.3.0")
    // Needed for JPMS modules
    implementation("io.smallrye.common:smallrye-common-function:2.1.0")
    implementation("io.smallrye.common:smallrye-common-expression:2.1.0")

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.guice)
    implementation(libs.javalin)
    implementation(libs.jetty.proxy)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jdbi3.core)
    implementation(libs.reactor.core)
    runtimeOnly(libs.h2database)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.spring.boot.starter.test)
    testRuntimeOnly(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)

    // This is somehow needed to make JPMS work with Spring
    implementation("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1")
    implementation("org.hibernate.common:hibernate-commons-annotations:6.0.6.Final")
    implementation("com.fasterxml:classmate")
    implementation("net.bytebuddy:byte-buddy")
}

configurations.all() {
    // the aop package is aleady contained in spring-aop
    exclude(group = "aopalliance", module = "aopalliance")
    exclude(group = "commons-logging", module = "commons-logging") // TODO check
    exclude(group = "org.slf4j", module = "slf4j-simple") // TODO this shoudn't be necessary
    exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j2-impl") // TODO this neither
}

application {
    mainClass.set("energy.eddie.framework.Framework")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.register("run-framework", JavaExec::class) {
    mainClass.set("energy.eddie.framework.Framework")
    classpath = sourceSets["main"].runtimeClasspath
    systemProperties.set("developmentMode", "true")
    workingDir = parent?.projectDir ?: projectDir
    group = "development"
    description = "run the EDDIE framework"
    environment["JDBC_URL"] = "jdbc:h2:tcp://localhost/./examples/example-app"
    environment["PUBLIC_CONTEXT_PATH"] = ""
    environment["FRAMEWORK_PORT"] = 8080
    environment["IMPORT_CONFIG_FILE"] = "file:./framework/src/test/resources/data-needs.yml"
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