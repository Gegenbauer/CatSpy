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
    implementation(Squareup.okio)

    testImplementation(kotlinTestApi())
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}