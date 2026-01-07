package energy.eddie

tasks.withType<JavaCompile>().configureEach {
    dependsOn(":pnpmBuild")
}

tasks.withType<ProcessResources>().configureEach {
    dependsOn(":pnpmBuild")
}
