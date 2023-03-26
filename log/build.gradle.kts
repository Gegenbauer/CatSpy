plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

group = "me.gegenbauer.logviewer"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(Weisj.darklafCore.group, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}