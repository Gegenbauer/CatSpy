plugins {
    kotlin("jvm")
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
    implementation(Weisj.darklafCore)
    testImplementation(Weisj.darklafCore)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}