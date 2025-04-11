plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.concurrency)
    implementation(projects.glog)
    implementation(projects.ui.utils)
    implementation(projects.javaext)
    implementation(Weisj.darklafCore)
    testImplementation(projects.concurrency)
    testImplementation(projects.glog)
    testImplementation(Weisj.darklafCore)
    testImplementation(JGoodies.binding)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}