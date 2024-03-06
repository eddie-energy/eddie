import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

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
    implementation(project(":outbound-kafka"))
    implementation(project(":region-connectors:shared"))
    implementation(project(":region-connectors:region-connector-aiida"))
    implementation(project(":region-connectors:region-connector-at-eda"))
    implementation(project(":region-connectors:region-connector-dk-energinet"))
    implementation(project(":region-connectors:region-connector-fr-enedis"))
    implementation(project(":region-connectors:region-connector-es-datadis"))
    implementation(project(":region-connectors:region-connector-simulation"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.websocket)
    implementation(libs.reactor.core)
    implementation(libs.flyway.core)


    runtimeOnly(libs.h2database)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.postgresql)


    testImplementation(libs.junit.jupiter)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.reactor.test)
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
    environment["IMPORT_CONFIG_FILE"] = "file:./core/src/test/resources/data-needs.yml"

    // when using PostgreSQL
    environment["JDBC_USER"] = "test"
    environment["JDBC_PASSWORD"] = "test"
    environment["JDBC_URL"] = "jdbc:postgresql://localhost:5432/eddie"
    environment["SPRING_JPA_DATABASE_PLATFORM"] = "org.hibernate.dialect.PostgreSQLDialect"

    // when using H2 database, no credentials are needed
    // environment["JDBC_URL"] = "jdbc:h2:tcp://localhost:9091/./examples/example-app"
    // environment["SPRING_JPA_DATABASE_PLATFORM"] = "org.hibernate.dialect.H2Dialect"
}

tasks.withType<JavaCompile>().configureEach {
    if (!name.lowercase(Locale.getDefault()).contains("test")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "energy.eddie")
        }
    }
}
