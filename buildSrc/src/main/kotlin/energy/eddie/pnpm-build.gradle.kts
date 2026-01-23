// SPDX-FileCopyrightText: 2023-2026 The EDDIE Developers <eddie.developers@fh-hagenberg.at>
// SPDX-License-Identifier: Apache-2.0

package energy.eddie

tasks.withType<JavaCompile>().configureEach {
    dependsOn(":pnpmBuild")
}

tasks.withType<ProcessResources>().configureEach {
    dependsOn(":pnpmBuild")
}
