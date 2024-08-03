plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.glog)
    implementation(projects.context)
    implementation(projects.cache)

    testImplementation(kotlinTestApi())
    testImplementation(Mockk.mockk)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}