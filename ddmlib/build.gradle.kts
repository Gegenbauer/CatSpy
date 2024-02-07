plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    api(Adam.adam)
    implementation(Slf4j.api)
    implementation(Logback.classic)
    implementation(projects.concurrency)
    implementation(projects.glog)
    implementation(projects.context)
}