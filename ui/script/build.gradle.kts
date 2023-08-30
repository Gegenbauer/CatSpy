import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.ui.configuration)
    implementation(projects.ui.utils)
    implementation(projects.ui.view)
    implementation(projects.ui.databinding)
    implementation(projects.resources)
    implementation(projects.glog)
    implementation(projects.concurrency)
    implementation(projects.task)
    implementation(projects.context)
    implementation(projects.ddmlib)
    implementation(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}