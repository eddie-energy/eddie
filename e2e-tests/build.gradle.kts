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
    implementation(libs.playwright)
    implementation(libs.jackson.databind)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testImplementation(libs.slf4j.simple)

    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.register("record-test", JavaExec::class) {
    description = "Record new playwright test"
    group = "playwright"
    mainClass.set("com.microsoft.playwright.CLI")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("codegen", "online.eddie.energy")
}

tasks.test {
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
