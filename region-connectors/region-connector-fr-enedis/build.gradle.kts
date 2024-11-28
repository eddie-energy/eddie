import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.util.*

plugins {
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
    implementation(project(":region-connectors:shared"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.retry)
    implementation(libs.spring.aspects)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.apache.http.client)
    implementation(libs.apache.http.mime)

    implementation(libs.reactor.core)
    implementation(libs.jakarta.validation.api)
    implementation(libs.jackson.annotations)

    runtimeOnly(libs.hibernate.validator)
    runtimeOnly(libs.jackson.databind.nullable)

    testImplementation(libs.reactor.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.okhttp3.mockwebserver)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.spring.boot.testcontainers)

    testRuntimeOnly(libs.flyway.core)
    testRuntimeOnly(libs.flyway.postgresql)
    testRuntimeOnly(libs.postgresql)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone.disableWarningsInGeneratedCode.set(true)
    if (!name.lowercase(Locale.getDefault()).contains("test") && !name.lowercase(Locale.getDefault()).contains("generated")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "energy.eddie.regionconnector.fr.enedis")
            option("NullAway:UnannotatedClasses", "energy.eddie.regionconnector.fr.enedis.api.MeteringDataApi")
            option("NullAway:UnannotatedClasses", "energy.eddie.regionconnector.fr.enedis.api.AuthorizationApi")
            option("NullAway:TreatGeneratedAsUnannotated", true)
            // Regex fits to Windows and Unix-style path separators. CAVEAT: excludedPaths needs a rexex string!
            val regexString = ".*/energy/eddie/regionconnector/fr/enedis/invoker/.*".replace("/", "[/\\\\]")
            this.excludedPaths.set(regexString)
            option("NullawayExcludedClasses=EnedisApiClient.java")
        }
    }
}

// disable bootJar task as it needs a main class and region connectors do not have one
tasks.getByName<BootJar>("bootJar") {
    enabled = false
}

tasks.getByName<Jar>("jar") {
    enabled = true
}
