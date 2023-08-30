plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    api(kotlin("reflect"))
    implementation(projects.glog)
}