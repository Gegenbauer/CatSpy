plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.glog)
    implementation(projects.file)
    implementation(projects.concurrency)
    implementation(projects.javaext)
}