plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.glog)
    implementation(projects.javaext)
    implementation(projects.context)
    implementation(projects.concurrency)
    implementation(Weisj.darklafCore)
}