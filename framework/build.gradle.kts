import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

plugins {
    application
    id("energy.eddie.java-conventions")
    id("org.gradlex.extra-java-module-info") version "1.3"
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
    implementation(project(":region-connectors:region-connector-simulation"))

    implementation(libs.microprofile.config)
    implementation("io.smallrye.config:smallrye-config:3.3.0")
    // Needed for JPMS modules
    implementation("io.smallrye.common:smallrye-common-function:2.1.0")
    implementation("io.smallrye.common:smallrye-common-expression:2.1.0")

    implementation(libs.spring.boot.starter.web)
    implementation(libs.guice)
    implementation(libs.javalin)
    implementation(libs.jetty.proxy)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jdbi3.core)
    implementation(libs.reactor.core)
    runtimeOnly(libs.h2database)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)
}

extraJavaModuleInfo {
    // This is needed because gradle puts jdbi3 on the classpath, not the module path.
    automaticModule("org.jdbi:jdbi3-core", "org.jdbi.v3.core")

    // These other two are needed, because the plugin extra-java-module-info would complain about them
    automaticModule("org.jetbrains:annotations", "org.jetbrains.annotations")
    automaticModule("org.jetbrains.kotlin:kotlin-stdlib-common", "kotlin.stdlib")
    automaticModule("com.google.inject:guice", "com.google.guice")
    automaticModule("javax.inject:javax.inject", "javax.inject")
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
    automaticModule(
        "com.google.errorprone:error_prone_type_annotations",
        "com.google.errorprone:error_prone_type_annotations"
    )
    automaticModule("org.pcollections:pcollections", "org.pcollections:pcollections")
    automaticModule("com.github.kevinstern:software-and-algorithms", "com.github.kevinstern:software-and-algorithms")
    automaticModule("org.eclipse.jgit:org.eclipse.jgit", "org.eclipse.jgit:org.eclipse.jgit")
    automaticModule("org.xerial.snappy:snappy-java", "snappy.java")
    automaticModule("org.apache.kafka:kafka-clients", "kafka.clients")
    automaticModule("commons-logging:commons-logging", "commons.logging")

    // Smallrye-Config does not support JPMS, other smallrye projects do
    automaticModule("io.smallrye.config:smallrye-config", "io.smallrye.config")
    automaticModule("io.smallrye.config:smallrye-config-core", "io.smallrye.config.core")
    automaticModule("io.smallrye.config:smallrye-config-common", "io.smallrye.config.common")
    automaticModule("org.eclipse.microprofile.config:microprofile-config-api", "eclipse.microprofile.config.api")
}

configurations.all() {
    // the aop package is aleady contained in spring-aop
    exclude(group = "aopalliance", module = "aopalliance")
    exclude(group = "commons-logging", module = "commons-logging") // TODO check
    exclude(group = "org.slf4j", module = "slf4j-simple") // TODO this shoudn't be necessary
    exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j2-impl") // TODO this neither
}

application {
    mainModule.set("energy.eddie.framework")
    mainClass.set("energy.eddie.framework.Framework")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.register("run-framework", JavaExec::class) {
    mainModule.set("energy.eddie.framework")
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