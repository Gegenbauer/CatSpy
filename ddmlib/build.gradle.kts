plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    api(AndroidDdm.groupName, AndroidDdm.ddmlib.artifact, AndroidDdm.ddmlib.version)
    api(Adam.groupName, Adam.adam.artifact, Adam.adam.version)
    implementation(Slf4j.groupName, Slf4j.simple.artifact, Slf4j.simple.version)
    compileOnly(projects.concurrency)
    compileOnly(kotlin("reflect"))
    compileOnly(projects.log)
    compileOnly(projects.task)
    compileOnly(projects.utils)
    compileOnly(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    testImplementation(projects.app)
    testImplementation(projects.concurrency)
    testImplementation(kotlin("reflect"))
    testImplementation(projects.log)
    testImplementation(projects.task)
    testImplementation(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}