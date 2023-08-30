plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.glog)
    implementation(projects.javaext)
    implementation(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
}