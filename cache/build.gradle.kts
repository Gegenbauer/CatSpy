plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.context)
    implementation(projects.glog)
    implementation(projects.javaext)

    testImplementation(kotlinTestApi())
}
