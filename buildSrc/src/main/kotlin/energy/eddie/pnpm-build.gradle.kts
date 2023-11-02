package energy.eddie

tasks.withType<JavaCompile>().configureEach {
    dependsOn(":pnpmBuild")
}