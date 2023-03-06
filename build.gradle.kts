plugins {
    id("java")
}

group = "energy.eddie"
version = "0.0.0"

allprojects {
    ext {
        set("junitVersion", "5.9.2")
    }
    repositories {
        mavenCentral()
    }
}





