plugins {
    id("java")
    id("energy.eddie.java-conventions")
}

group = "energy.eddie.tests.e2e"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kafka.clients)
    implementation("com.microsoft.playwright:playwright:1.40.0")
    testImplementation("org.slf4j:slf4j-simple:2.0.10")

    testImplementation(libs.junit.jupiter)
    implementation(libs.jackson.databind)
    testImplementation("org.assertj:assertj-core:3.25.2")
}

tasks.register("record-test", JavaExec::class) {
    description = "Record new playwright test"
    group = "playwright"
    mainClass.set("com.microsoft.playwright.CLI")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("codegen", "online.eddie.energy")
}

tasks.test {
    if (environment["CI"] == "true") {
        environment("EXAMPLE_APP", "http://eddie-example-app:8081/prototype/main")
    } else {
        environment("EXAMPLE_APP", "http://localhost:8081")
    }
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.register<JavaExec>("install-playwright-deps") {
    description = "Install playwright deps"
    group = "playwright"
    mainClass.set("com.microsoft.playwright.CLI")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("install", "--with-deps")
}

project(":e2e-tests") {
    tasks.test {
        onlyIf {
            project.hasProperty("run-e2e-tests")
        }
    }
}
