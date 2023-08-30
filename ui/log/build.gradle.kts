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
    implementation(projects.resources)
    implementation(projects.ddmlib)
    implementation(projects.glog)
    implementation(projects.task)
    implementation(projects.file)
    implementation(projects.concurrency)
    implementation(projects.context)
    implementation(projects.cache)
    implementation(projects.platform)
    implementation(projects.javaext)
    implementation(projects.ui.render)
    implementation(projects.ui.databinding)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}