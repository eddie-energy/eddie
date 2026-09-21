// SPDX-FileCopyrightText: 2025-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

import energy.eddie.cim.GenerateCimSchemaClassesTask
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

plugins {
    id("java")
    `maven-publish`
    jacoco
    signing
    `java-library`
}

group = "energy.eddie"

version = "3.12.0"

repositories {
    mavenCentral()
}

// JAXB configuration holds classpath for running the JAXB XJC compiler
val jaxb = configurations.create("jaxb")

dependencies {
    implementation(platform(libs.jackson.bom))
    implementation(libs.jackson.core)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.annotations)
    implementation(libs.jackson.dataformat.xml)
    implementation(libs.jackson.jakarta.xmlbind.annotations)
    implementation(libs.jaxb.runtime)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.jakarta.validation.api)
    implementation(libs.jakarta.xml.bind.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.xmlunit.core)
    testRuntimeOnly(libs.junit.platform.launcher)

    // dependencies needed to generate code
    jaxb(libs.jaxb.xjc)
    jaxb(libs.jaxb.runtime)
    jaxb(libs.jaxb.plugins)
    jaxb(libs.jaxb.plugin.annotate)
    jaxb(libs.jackson.annotations)
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.withType<JacocoReport>())
}

tasks.withType<Test>().configureEach {
}

tasks.withType<JacocoReport> {
    reports {
        xml.required.set(true)
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

val generateCIMSchemaClasses = tasks.register<GenerateCimSchemaClassesTask>("generateCIMSchemaClasses") {
    description = "Generate CIM Java Classes from XSD files"
    group = "Build"

    // ordered schema files to prevent repeated generation of Java classes.
    val orderedSchemaFiles = listOf(
        // V0.82
        "v0_82/vhd/ValidatedHistoricalData_MarketDocument_2024-06-21T12.10.53.xsd",
        "v0_82/ap/AccountingPoint_MarketDocument_2024-06-21T11.38.58.xsd",
        "v0_82/pmd/Permission_Envelope_2024-06-21T11.51.02.xsd",
        // V0.91.08
        "v0_91_08/RedistributionTransactionRequest Document_Annotated.xsd",
        // V1.04
        "v1_04/vhd/ValidatedHistoricalData Document_v1.04_annotated.xsd",
        "v1_04/rtd/RealTimeData Document_v1.04_Annotated.xsd",
        "v1_04/pmd/Permission Document_v1.04_annotated.xsd",
        "v1_04/ap/AccountingPointData Document_v1.04_annotated.xsd",
        // V1.12
        "v1_12/rtd/RealTimeData Document_v1.12_annotated.xsd",
        "v1_12/recmmoe/ReferenceEnergyCurveMinMaxOperatingEnvelope Document_v1.12_annotated.xsd",
        "v1_12/ack/Acknowledgement Document_v1.12_annotated.xsd",
        "v1_12/esr/CEEDS_EnergySharingReferenceDataMarketDocument_annotated_v1.12.xsd",
        "v1_12/rpmd/RequestPermissionDocument_annotated_v1.12.xsd"
    )

    schemaDirectory.set(layout.projectDirectory.dir("src/main/schemas/cim/xsd"))
    entryPointSchemas.set(orderedSchemaFiles)
    jaxbClasspath.from(jaxb)
    outputDirectory.set(layout.buildDirectory.dir("generated/sources/xjc/main/java"))
}

// Add generated sources to the main source set
sourceSets {
    main {
        java {
            srcDir(generateCIMSchemaClasses)
        }
        resources {
            srcDir("${projectDir}/src/main/schemas")
        }
    }
    test {
        java {
            srcDir(generateCIMSchemaClasses)
        }
        resources {
            srcDir("${projectDir}/src/main/schemas")
        }
    }
}

tasks.compileJava {
    // generate the classes before compiling
    dependsOn(generateCIMSchemaClasses)
}

val mavenCentralUsername: String? = System.getenv("MAVEN_CENTRAL_USERNAME")
val mavenCentralPassword: String? = System.getenv("MAVEN_CENTRAL_PASSWORD")
publishing {
    repositories {
        maven {
            name = "MavenCentral"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = mavenCentralUsername
                password = mavenCentralPassword
            }
        }
    }
    publications {
        create<MavenPublication>("cim") {
            from(components["java"])
            groupId = group.toString()
            artifactId = project.name

            pom {
                name = project.name
                description = "Generated CIM classes and helpers"
                url = "https://github.com/eddie-energy/eddie"

                licenses {
                    license {
                        name = "Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }

                developers {
                    developer {
                        id = "eddie-energy"
                        name = "EDDIE Developers"
                        email = "developers@eddie.energy"
                    }
                }

                scm {
                    connection = "scm:git:https://github.com/eddie-energy/eddie.git"
                    developerConnection = "scm:git:ssh://github.com/eddie-energy/eddie.git"
                    url = "https://github.com/eddie-energy/eddie"
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("MAVEN_CENTRAL_SIGNING_KEY"),
        System.getenv("MAVEN_CENTRAL_SIGNING_KEY_PASSWORD")
    )
    sign(publishing.publications["cim"])
}

tasks.register("publishCimPublicationToMavenRepository") {
    group = "publishing"
    description = "Releases the software from the staging area to the maven central repository"
    doLast {
        val token = Base64.getEncoder()
            .encodeToString(("$mavenCentralUsername:$mavenCentralPassword").toByteArray())

        val url =
            "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/energy.eddie?publishing_type=automatic"

        val response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder()
                .uri(uri(url))
                .header("accept", "*/*")
                .header("Authorization", "Bearer $token")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build(),
            HttpResponse.BodyHandlers.ofString()
        )

        if (response.statusCode() !in 200..299) {
            logger.error("Upload failed: ${response.statusCode()} ${response.body()}")
            throw IllegalStateException("Upload failed: ${response.statusCode()} ${response.body()}")
        }

        logger.info("Upload successful (${response.statusCode()})")
    }
}

tasks.withType<Javadoc>().configureEach {
    val opt = options as StandardJavadocDocletOptions
    // Disable linting in generated CIM classes, since XJC does not escape certain characters properly, such as quotation marks or ampersands.
    opt.addStringOption("Xdoclint/package:-energy.eddie.cim.*", "-quiet")
}
