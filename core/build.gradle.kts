import energy.eddie.configureJavaCompileWithErrorProne

plugins {
    application
    id("energy.eddie.java-conventions")
    id("energy.eddie.pnpm-build")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "energy.eddie"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))
    implementation(project(":data-needs"))
    implementation(project(":admin-console"))
    implementation(project(":european-masterdata"))
    implementation(project(":outbound-connectors:outbound-kafka"))
    implementation(project(":outbound-connectors:outbound-amqp"))
    implementation(project(":outbound-connectors:outbound-rest"))
    implementation(project(":outbound-connectors:outbound-shared"))
    implementation(project(":region-connectors:shared"))
    implementation(project(":region-connectors:region-connector-aiida"))
    implementation(project(":region-connectors:region-connector-at-eda"))
    implementation(project(":region-connectors:region-connector-be-fluvius"))
    implementation(project(":region-connectors:region-connector-dk-energinet"))
    implementation(project(":region-connectors:region-connector-fr-enedis"))
    implementation(project(":region-connectors:region-connector-es-datadis"))
    implementation(project(":region-connectors:region-connector-nl-mijn-aansluiting"))
    implementation(project(":region-connectors:region-connector-fi-fingrid"))
    implementation(project(":region-connectors:region-connector-simulation"))
    implementation(project(":region-connectors:region-connector-us-green-button"))
    implementation(project(":region-connectors:region-connector-cds"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.websocket)
    implementation(libs.spring.openapi.webmvc.ui)
    implementation(libs.spring.boot.security)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.reactor.core)
    implementation(libs.flyway.core)

    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.postgresql)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.reactor.test)
    testRuntimeOnly(libs.h2database)
}

configurations.all {
    exclude(group = "commons-logging", module = "commons-logging") // TODO check
    exclude(group = "org.slf4j", module = "slf4j-simple") // TODO this shoudn't be necessary
    exclude(group = "org.apache.logging.log4j", module = "log4j-slf4j2-impl") // TODO this neither
}

application {
    mainClass.set("energy.eddie.EddieSpringApplication")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.register("run-core", JavaExec::class) {
    dependsOn(":pnpmBuild")
    mainClass.set("energy.eddie.EddieSpringApplication")
    classpath = sourceSets["main"].runtimeClasspath
    systemProperties["developmentMode"] = "true"
    workingDir = parent?.projectDir ?: projectDir
    group = "development"
    description = "run EDDIE with Spring"

    environment["CORE_PORT"] = 8080
    environment["JDBC_USER"] = "test"
    environment["JDBC_PASSWORD"] = "test"
    environment["JDBC_URL"] = "jdbc:postgresql://localhost:5432/eddie"
}

configureJavaCompileWithErrorProne("energy.eddie.core")
