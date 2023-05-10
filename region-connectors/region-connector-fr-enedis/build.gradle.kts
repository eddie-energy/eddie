import net.ltgt.gradle.errorprone.CheckSeverity
import net.ltgt.gradle.errorprone.errorprone
import org.hidetake.gradle.swagger.generator.GenerateSwaggerCode
import java.util.*

plugins {
    id("energy.eddie.java-conventions")
    id("java")
    id("idea")
    id("org.hidetake.swagger.generator") version "2.19.2"
}

group = "energy.eddie"
version = "0.0.0"

repositories {
    mavenCentral()
}

dependencies {
    swaggerCodegen(libs.swagger.codegen)

    implementation(libs.swagger.codegen)
    implementation(libs.javax.annotation.api)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.threetenbp)
    implementation(libs.gson)
    implementation(libs.gson.fire)
    implementation(libs.dotenv)

    implementation(project(mapOf("path" to ":region-connectors:region-connector-api")))
}

// Directory for generated java files
val generatedSwaggerJavaDir = "${buildDir.absolutePath}/generated/sources/swagger/main/java"

// Add generated sources to the main source set
sourceSets {
    main {
        java {
            srcDir(generatedSwaggerJavaDir)
        }
    }
    test {
        java {
            srcDir(generatedSwaggerJavaDir)
        }
    }
}

tasks.register<GenerateSwaggerCode>("enedisApiClient") {
    val apiClientInputFile = "${projectDir}/src/main/resources/enedis-api-client.json"
    val supportingFiles =
            "supportingFiles=" +
                    "ApiCallback.java," +
                    "ApiClient.java," +
                    "ApiException.java," +
                    "ApiResponse.java," +
                    "Configuration.java," +
                    "JSON.java," +
                    "Pair.java," +
                    "ProgressRequestBody.java," +
                    "ProgressResponseBody.java," +
                    "StringUtil.java," +
                    // Auth
                    "ApiKeyAuth.java," +
                    "Authentication.java," +
                    "HttpBasicAuth.java," +
                    "OAuth.java," +
                    "OAuthFlow.java"

    inputFile = file(apiClientInputFile)
    outputDir = file(generatedSwaggerJavaDir)
    language = "java"
    additionalProperties = mapOf(
            "apiPackage" to "energy.eddie.regionconnector.fr.enedis.api",
            "modelPackage" to "energy.eddie.regionconnector.fr.enedis.model",
            "authPackage" to "energy.eddie.regionconnector.fr.enedis.auth",
            "sourceFolder" to "/",
            "dateLibrary" to "java8"
    )
    components = listOf("models", "apis", "auths", "apiTests=false", supportingFiles)
}

tasks.named("compileJava").configure {
    dependsOn(tasks.named("enedisApiClient"))
}


tasks.withType<JavaCompile>().configureEach {
    if (!name.lowercase(Locale.getDefault()).contains("test") && !name.lowercase(Locale.getDefault()).contains("generated")) {
        options.errorprone {
            check("NullAway", CheckSeverity.ERROR)
            option("NullAway:AnnotatedPackages", "energy.eddie.regionconnector.fr.enedis.client")
        }
    }
}