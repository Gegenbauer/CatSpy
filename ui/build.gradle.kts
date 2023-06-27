plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    api(projects.ui.log)
    api(projects.ui.script)
    compileOnly(projects.glog)
    compileOnly(kotlin("reflect"))
    compileOnly(projects.concurrency)
    compileOnly(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    testImplementation(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}