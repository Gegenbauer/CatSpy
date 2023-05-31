plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    compileOnly(projects.log)
    compileOnly(projects.utils)
    compileOnly(projects.databinding)
    compileOnly(kotlin("reflect"))
    api(Kotlin.groupName, Kotlin.coroutineCore.artifact, Kotlin.coroutineCore.version)
    api(Kotlin.groupName, Kotlin.coroutineSwing.artifact, Kotlin.coroutineSwing.version)
    api(Fifesoft.groupName, Fifesoft.autocomplete.artifact, Fifesoft.autocomplete.version)

    testImplementation(kotlin("test"))
    testImplementation(projects.utils)
    testImplementation(projects.databinding)
    testImplementation(kotlin("reflect"))
    testImplementation(Mockk.groupName, Mockk.mockk.artifact, Mockk.mockk.version)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}