plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    api(kotlinReflectApi())
    implementation(projects.javaext)
    implementation(projects.file)
    implementation(projects.glog)
    implementation(projects.concurrency)
    testImplementation(kotlinTestApi())
}
