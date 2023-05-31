plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    compileOnly(projects.log)
    compileOnly(projects.utils)
    compileOnly(projects.databinding)
    api(Kotlin.groupName, Kotlin.coroutineCore.artifact, Kotlin.coroutineCore.version)
    api(Kotlin.groupName, Kotlin.coroutineSwing.artifact, Kotlin.coroutineSwing.version)
    api(Fifesoft.groupName, Fifesoft.autocomplete.artifact, Fifesoft.autocomplete.version)

    testImplementation(kotlin("test"))
    testImplementation(projects.utils)
    testImplementation(projects.databinding)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}