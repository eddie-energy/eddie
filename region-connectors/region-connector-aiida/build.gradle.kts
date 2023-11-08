import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

plugins {
    id("energy.eddie.java-conventions")
    id("energy.eddie.pnpm-build")
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
    implementation(project(":region-connectors:shared"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)

    implementation(libs.microprofile.config)
    implementation(libs.reactor.core)


    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
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
    automaticModule(
        "com.google.errorprone:error_prone_type_annotations",
        "com.google.errorprone:error_prone_type_annotations"
    )
    automaticModule("org.pcollections:pcollections", "org.pcollections:pcollections")
    automaticModule("com.github.kevinstern:software-and-algorithms", "com.github.kevinstern:software-and-algorithms")
    automaticModule("org.eclipse.jgit:org.eclipse.jgit", "org.eclipse.jgit:org.eclipse.jgit")

    automaticModule("org.eclipse.microprofile.config:microprofile-config-api", "eclipse.microprofile.config.api")
    automaticModule("org.springframework:spring-aspects", "springframework.spring.aspects")

    // for spring test
    automaticModule("com.jayway.jsonpath:json-path", "")
    automaticModule("net.minidev:json-smart", "")
    automaticModule("org.skyscreamer:jsonassert", "")
    automaticModule("net.minidev:accessors-smart", "")
    automaticModule("com.vaadin.external.google:android-json", "")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    if (!name.lowercase(Locale.getDefault()).contains("test")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "energy.eddie.regionconnector.aiida")
        }
    }
}