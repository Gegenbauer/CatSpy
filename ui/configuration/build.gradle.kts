import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.ui.utils)
    implementation(projects.ui.databinding)
    implementation(projects.concurrency)
    implementation(projects.resources.iconset)
    implementation(projects.platform)
    implementation(projects.ddmlib)
    implementation(projects.task)
    implementation(projects.glog)
    implementation(Gson.groupName, Gson.gson.artifact, Gson.gson.version)
    implementation(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
}

