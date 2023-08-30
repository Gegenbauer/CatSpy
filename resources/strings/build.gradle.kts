plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(Gson.groupName, Gson.gson.artifact, Gson.gson.version)
    implementation(projects.file)
    implementation(projects.resources.common)
    implementation(projects.ui.utils)
}