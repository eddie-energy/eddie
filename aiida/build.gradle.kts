import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("energy.eddie.java-conventions")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.google.jib)
}

group = "energy.eddie.aiida"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.data.jpa)

    implementation(libs.reactor.core)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgresql)
    implementation(libs.spring.openapi.webmvc.ui)
    implementation(libs.kafka.clients)
    implementation(libs.eclipse.paho.mqttv5.client)
    // enable Jackson support to fetch Hibernate lazy loaded properties when serializing
    implementation(libs.jackson.hibernate6)

    runtimeOnly(libs.postgresql)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.reactor.test)
    testImplementation(libs.apache.http.client)
    testImplementation(libs.hakky.logcaptor)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.toxiproxy)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
}

configurations {
    testImplementation {
        // disable logging modules of spring for tests, so that we can use slf4j2-mock to test loggers
        exclude("org.springframework.boot", "spring-boot-starter-logging")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    if (!name.lowercase().contains("test")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "energy.eddie.aiida")
        }
    }
}

tasks.named<Test>("test") {
    description = "Runs all tests except integration tests."
    useJUnitPlatform {
        filter {
            // exclude all integration tests
            excludeTestsMatching("*IntegrationTest")
        }
    }

    testLogging {
        events("passed")
    }
}

tasks.register<Test>("integrationTest") {
    description = "Runs only integration tests."
    group = "verification"
    useJUnitPlatform {
        filter {
            includeTestsMatching("*IntegrationTest")
        }
    }

    testLogging {
        events("passed")
    }
}

tasks.withType<Test>().configureEach {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.withType<JacocoReport> {
    reports {
        xml.required.set(true)
    }
}


jib {
    from {
        image = "eclipse-temurin:21"
    }
    to {
        image = System.getProperty("jib.to.image")
        auth {
            username = "oauth2accesstoken"
            password = System.getProperty("jib.to.auth.password")
        }
    }
    container {
        // Using the current timestamp as image creation date instead of Unix timestamp 0.
        // This leads to not reproducible build timestamps, if this is desired, remove the line.
        // see https://github.com/GoogleContainerTools/jib/blob/master/docs/faq.md#why-is-my-image-created-48-years-ago
        creationTime = "USE_CURRENT_TIMESTAMP"
    }
}
