plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    compileOnly(Weisj.darklafCore.group, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}