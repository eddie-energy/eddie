import com.github.gradle.node.pnpm.task.PnpmTask
import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.InventoryHtmlReportRenderer
import com.github.jk1.license.render.ReportRenderer

plugins {
    id("java")
    id("com.github.node-gradle.node") version "5.0.0"
    id("com.github.jk1.dependency-license-report") version "2.5"
    id("org.sonarqube")
    jacoco
}

repositories {
    mavenCentral()
}

group = "energy.eddie"
version = "0.0.0"

dependencies {
    runtimeOnly(libs.h2database)
}

node {
    version.set("18.14.0")
    pnpmVersion.set("8.5.0")
    download.set(true)
}

licenseReport {
    renderers = arrayOf<ReportRenderer>(InventoryHtmlReportRenderer("report.html", "EDDIE Framework"))
    filters = arrayOf<DependencyFilter>(LicenseBundleNormalizer())
}

tasks.register("run-db-server-create-db", JavaExec::class) {
    mainClass.set("org.h2.tools.Shell")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("-url", "jdbc:h2:./examples/example-app")
    group = "development"
    description = "create the local H2 database"
}

tasks.register("run-db-server", JavaExec::class) {
    dependsOn("run-db-server-create-db")
    mainClass.set("org.h2.tools.Server")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("-tcp", "-tcpPort", "9091", "-web")
    group = "development"
    description = "run the H2 db server"
}

tasks.register<PnpmTask>("pnpmBuild") {
    dependsOn("pnpmInstall")
    pnpmCommand.set(listOf("run", "build"))
}

sonar {
    properties {
        property("sonar.projectName", "EDDIE")
        property("sonar.projectKey", "eddie-energy")
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            "${project.rootDir}/build/reports/jacoco/codeCoverageReport/codeCoverageReport.xml"
        )
    }
}

// Taken from https://github.com/SonarSource/sonar-scanning-examples/blob/master/sonar-scanner-gradle/gradle-multimodule-coverage/build.gradle
tasks.register<JacocoReport>("codeCoverageReport") {
    description = "Generates a jacoco report for all subprojects."
    group = "verification"
    // If a subproject applies the 'jacoco' plugin, add the result of it to the report
    subprojects {
        val subproject = this
        subproject.plugins.withType<JacocoPlugin>().configureEach {
            subproject.tasks.matching { it.extensions.findByType<JacocoTaskExtension>() != null }.configureEach {
                val testTask = this
                sourceSets(subproject.sourceSets.main.get())
                executionData(testTask)
            }
        }
    }

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}