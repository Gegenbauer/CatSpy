plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    compileOnly(projects.glog)
    api(Kotlin.groupName, Kotlin.coroutineCore.artifact, Kotlin.coroutineCore.version)
    api(Kotlin.groupName, Kotlin.coroutineSwing.artifact, Kotlin.coroutineSwing.version)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}