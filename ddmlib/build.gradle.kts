plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    api(AndroidDdm.ddmlib)
    api(Adam.adam)
    implementation(Slf4j.simple)
    implementation(projects.concurrency)
    implementation(projects.glog)
    implementation(projects.context)
}