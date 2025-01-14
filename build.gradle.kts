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


node {
    version.set("18.14.0")
    pnpmVersion.set("8.5.0")
    download.set(true)
}

licenseReport {
    renderers = arrayOf<ReportRenderer>(InventoryHtmlReportRenderer("report.html", "EDDIE Framework"))
    filters = arrayOf<DependencyFilter>(LicenseBundleNormalizer())
}

tasks.register<PnpmTask>("pnpmBuild") {
    group = "build"
    description = "builds the eddie button and custom elements"
    dependsOn("pnpmInstall")
    pnpmCommand.set(listOf("run", "build"))
}

tasks.register<PnpmTask>("pnpmBuildAdminConsole") {
    group = "build"
    description = "builds and bundles the admin console ui"
    dependsOn("pnpmInstall")
    pnpmCommand.set(listOf("run", "admin:build"))
    environment = System.getenv()

    doLast {
        copy {
            from("admin-console/ui/dist")
            into("admin-console/src/main/resources/public")
        }
    }
}

tasks.register<PnpmTask>("pnpmBuildDocs") {
    group = "documentation"
    description = "builds the eddie framework documentation"
    dependsOn("pnpmInstall")
    pnpmCommand.set(listOf("run", "docs:build"))
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

val projDef = arrayListOf<Project>()
rootProject.subprojects.forEach { subproject ->
    subproject.plugins.withId("java") {
        projDef.add(subproject)
    }
}
tasks.register<Javadoc>("allJavadoc") {
    group = "documentation"
    description = "Generates a combined javadoc of all subprojects."
    projDef.forEach {
        dependsOn(it.tasks.named("compileJava"))
    }
    val fileTree: List<File> = (projDef.flatMap { it.sourceSets.main.get().allJava })
    setSource(fileTree)
    classpath = files(projDef.flatMap { it.sourceSets.main.get().compileClasspath })
    setDestinationDir(file("${project.rootDir}/build/docs/javadoc-all"))
    val opt = options as StandardJavadocDocletOptions
    // Disable linting in generated CIM classes
    opt.addStringOption("Xdoclint/package:-energy.eddie.cim.*", "-quiet")
}
