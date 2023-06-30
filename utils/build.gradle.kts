plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(Gson.groupName, Gson.gson.artifact, Gson.gson.version)
    compileOnly(projects.glog)
    compileOnly(kotlin("reflect"))
    compileOnly(projects.concurrency)
    compileOnly(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)

    testImplementation(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    testImplementation(kotlin("test"))
    testImplementation(Mockk.groupName, Mockk.mockk.artifact, Mockk.mockk.version)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}