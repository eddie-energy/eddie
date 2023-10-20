package energy.eddie

import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.withType

tasks.withType<JavaCompile>().configureEach {
    finalizedBy(":pnpmBuild")
}