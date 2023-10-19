import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
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
    implementation(Weisj.darklafCore)
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}