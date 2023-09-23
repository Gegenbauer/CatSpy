plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    api(kotlinReflectApi())
    implementation(projects.glog)
    testImplementation(kotlinTestApi())
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}