plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    api(AndroidDdm.groupName, AndroidDdm.ddmlib.artifact, AndroidDdm.ddmlib.version)
    api(Adam.groupName, Adam.adam.artifact, Adam.adam.version)
    implementation(Slf4j.groupName, Slf4j.simple.artifact, Slf4j.simple.version)
    implementation(projects.concurrency)
    implementation(projects.glog)
    implementation(projects.context)
}