plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.glog)

    testImplementation(kotlinTestApi())
    testImplementation(Mockk.mockk)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}