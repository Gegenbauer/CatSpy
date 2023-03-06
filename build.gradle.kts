plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "me.gegenbauer"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(8)
    sourceSets {
        dependencies {
            implementation(files("libs/flatlaf-2.1.jar"))
            implementation("com.github.weisj:darklaf-core:3.0.2")
        }
    }
}

application {
    mainClass.set("MainKt")
}