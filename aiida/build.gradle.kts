import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("org.springframework.boot") version "3.1.2"
    id("io.spring.dependency-management") version "1.1.2"
    id("net.ltgt.errorprone") version "3.0.1"
    id("org.sonarqube") version "4.0.0.2929"
    id("com.google.cloud.tools.jib") version "3.4.0"
    jacoco
}

group = "energy.eddie.aiida"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    runtimeOnly("org.postgresql:postgresql:42.6.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // errorprone & nullAway
    annotationProcessor("com.uber.nullaway:nullaway:0.10.11")
    // Optional, some source of nullability annotations.
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    errorprone("com.google.errorprone:error_prone_core:2.18.0")
    errorproneJavac("com.google.errorprone:javac:9+181-r4173-1")

    // testcontainers
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:toxiproxy")
    testImplementation("org.testcontainers:kafka")

    // Kafka
    implementation("org.apache.kafka:kafka-clients:3.2.3")

    // reactor
    implementation("io.projectreactor:reactor-core:3.5.10")
    testImplementation("io.projectreactor:reactor-test:3.5.10")

    // catches invalid hibernate validation annotations, e.g. a String annotated with @Past
    annotationProcessor("org.hibernate.validator:hibernate-validator-annotation-processor:8.0.1.Final")

    // required for making PATCH requests in integration test
    testImplementation("org.apache.httpcomponents.client5:httpclient5:5.2.1")

    // test interaction with logger
    testImplementation("io.github.hakky54:logcaptor:2.9.0")

    // mqtt
    implementation("org.eclipse.paho:org.eclipse.paho.mqttv5.client:1.2.5")

    // generate openAPI documentation for swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // execute database scripts and migrations
    implementation("org.flywaydb:flyway-core:9.22.3")
}

dependencyManagement {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:1.19.1")
    }
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
            option("NullAway:AnnotatedPackages", "energy.eddie")
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

jacoco {
    toolVersion = "0.8.9"
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
        image = "eclipse-temurin:19"
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