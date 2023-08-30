plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    api(projects.ui.log)
    api(projects.ui.script)
    api(projects.ui.configuration)
    api(projects.ui.databinding)
    api(projects.ui.utils)
    api(projects.ui.view)
    api(projects.ui.filter)
    implementation(projects.glog)
    implementation(projects.concurrency)
    implementation(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    testImplementation(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}