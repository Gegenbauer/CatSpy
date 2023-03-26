plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    compileOnly(projects.log)
    api(Kotlin.coroutineCore.group, Kotlin.coroutineCore.artifact, Kotlin.coroutineCore.version)
    api(Kotlin.coroutineSwing.group, Kotlin.coroutineSwing.artifact, Kotlin.coroutineSwing.version)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}