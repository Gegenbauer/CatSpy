plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    api(kotlinReflectApi())
    implementation(projects.glog)
    implementation(projects.concurrency)
    testImplementation(kotlinTestApi())
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}