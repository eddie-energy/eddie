import com.github.gradle.node.pnpm.task.PnpmTask

plugins {
    id("java")
    id("com.github.node-gradle.node") version "5.0.0"
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
    args = listOf("-tcp", "-web")
    group = "development"
    description = "run the H2 db server"
}

tasks.register<PnpmTask>("pnpmBuild") {
    dependsOn("pnpmInstall")
    pnpmCommand.set(listOf("run", "build"))
}