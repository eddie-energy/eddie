plugins {
    id("java")
}

group = "energy.eddie"
version = "0.0.0"

repositories {
    mavenCentral()
}

// JAXB configuration holds classpath for running the JAXB XJC compiler
val jaxb: Configuration by configurations.creating

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${project.ext["junitVersion"]}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${project.ext["junitVersion"]}")

    // dependencies needed to generate code
    jaxb("org.glassfish.jaxb:jaxb-xjc:4.0.2")
    jaxb("org.glassfish.jaxb:jaxb-runtime:4.0.2")
    // The next two dependencies are not necessary for generating code, only when running the code:
    // Generated code depends on the JAXB API, which is removed from base Java in JDK 11, and therefore needs to be added
    implementation("org.glassfish.jaxb:jaxb-runtime:4.0.2")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0") // In JAXB v3, 'javax.xml.bind' was moved to 'jakarta.xml.bind'
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.0")
}


tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

// Directory for generated java files
val generatedXJCJavaDir = "${buildDir.absolutePath}/generated/sources/xjc/main/java"

// Add generated sources to the main source set
sourceSets {
    main {
        java {
            srcDir(generatedXJCJavaDir)
        }
    }
}

val generateEDASchemaClasses = tasks.create<JavaExec>("generateEDASchemaClasses") {
    description = "Generate EDA Java Classes from XSD files"
    classpath(jaxb)
    mainClass.set("com.sun.tools.xjc.XJCFacade")

    // make sure the directory exists
    file(generatedXJCJavaDir).mkdirs()

    // Path to XSD files
    val edaSchemaFiles = "src/main/schemes/eda/xsd/"

    args("-d", generatedXJCJavaDir, edaSchemaFiles, "-mark-generated", "-npa")
}


tasks.named("compileJava") {
    // generate the classes before compiling
    dependsOn(generateEDASchemaClasses)
}