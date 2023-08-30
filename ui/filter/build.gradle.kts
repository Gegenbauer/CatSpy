plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    implementation(projects.glog)
    implementation(projects.ui.utils)
    implementation(projects.javaext)
    api(Kotlin.groupName, Kotlin.coroutineCore.artifact, Kotlin.coroutineCore.version)
    api(Kotlin.groupName, Kotlin.coroutineSwing.artifact, Kotlin.coroutineSwing.version)
    api(Fifesoft.groupName, Fifesoft.autocomplete.artifact, Fifesoft.autocomplete.version)

    testImplementation(kotlin("test"))
    testImplementation(projects.ui.utils)
    testImplementation(kotlin("reflect"))
    testImplementation(Mockk.groupName, Mockk.mockk.artifact, Mockk.mockk.version)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}