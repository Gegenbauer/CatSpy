plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(Gson.groupName, Gson.gson.artifact, Gson.gson.version)
    compileOnly(projects.log)
    compileOnly(kotlin("reflect"))
    compileOnly(projects.concurrency)
    compileOnly(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    testImplementation(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}