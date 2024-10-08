plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    api(Adam.adam)
    implementation(Logback.classic)
    implementation(projects.concurrency)
    implementation(projects.glog)
    implementation(projects.context)
    implementation(projects.platform)
    implementation(projects.file)
    implementation(projects.javaext)
}