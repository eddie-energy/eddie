import energy.eddie.configureJavaCompileWithErrorProne

plugins {
    java
    id("energy.eddie.java-conventions")
    id("energy.eddie.pnpm-build")
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
    implementation(project(":data-needs"))

    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.autoconfigurer)
    implementation(libs.spring.openapi.webmvc.ui)
    implementation(libs.spring.boot.security)
    implementation(libs.spring.boot.starter.keycloak)
    implementation(libs.spring.boot.starter.thymeleaf)
    implementation(libs.spring.boot.starter.security.oauth2.client)
    implementation(libs.spring.boot.starter.security.oauth2.resource.server)

    implementation(libs.reactor.core)
    implementation(libs.eclipse.paho.mqttv5.client)
    // enable Jackson support to fetch Hibernate lazy loaded properties when serializing
    implementation(libs.jackson.hibernate7)
    implementation(libs.jackson.jakarta.xmlbind.annotations)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.j2mod)
    implementation(libs.mvel2)

    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.postgresql)
    runtimeOnly(libs.spring.boot.starter.flyway)

    testImplementation(libs.spring.security.test)
    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.starter.data.jpa.test)
    testImplementation(libs.spring.boot.starter.security.oauth2.resource.server.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.reactor.test)
    testImplementation(libs.apache.http.client)
    testImplementation(libs.hakky.logcaptor)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.toxiproxy)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.spring.boot.starter.flyway)
    testImplementation(libs.junit.mockito)

    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

configureJavaCompileWithErrorProne("energy.eddie.aiida")

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

tasks.withType<JavaCompile>().configureEach {
    dependsOn(":pnpmBuildAiida")
}

tasks.withType<ProcessResources>().configureEach {
    dependsOn(":pnpmBuildAiida")
}

jib {
    from {
        image = "eclipse-temurin:21"
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
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
