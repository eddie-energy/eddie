import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import java.util.*

plugins {
    id("java")
    id("energy.eddie.java-conventions")
}

group = "energy.eddie"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.mockito)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)


    implementation(libs.jakarta.annotation.api)


    implementation(libs.reactor.core)
    testImplementation(libs.reactor.test)

    implementation(libs.slf4j.api)

    implementation(libs.jakarta.annotation.api)
    // https://mvnrepository.com/artifact/io.projectreactor.netty/reactor-netty
    implementation("io.projectreactor.netty:reactor-netty:1.1.10")
    // https://mvnrepository.com/artifact/io.netty/netty-codec-http
    implementation("io.netty:netty-codec-http:4.1.97.Final")

}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    if (!name.lowercase(Locale.getDefault()).contains("test")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "energy.eddie.regionconnector.es")
        }
    }
}