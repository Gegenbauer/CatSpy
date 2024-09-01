plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.glog)
    implementation(projects.file)
    implementation(projects.javaext)
    implementation(projects.context)
    implementation(projects.concurrency)
    implementation(projects.platform)
    implementation(projects.cache)
    implementation(FormDev.flatLaf)
    implementation(Weisj.darklafCore)

    testImplementation(kotlinTestApi())
}