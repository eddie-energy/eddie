plugins {
    id("java")
}

repositories {
    mavenCentral()
}

group = "energy.eddie"
version = "0.0.0"

dependencies {
    runtimeOnly(libs.h2database)
}

tasks.register("run-db-server-create-db", JavaExec::class) {
    mainClass.set("org.h2.tools.Shell")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("-url", "jdbc:h2:./ep-demo-app")
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
