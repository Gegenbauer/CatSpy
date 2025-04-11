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
    implementation(projects.javaext)
    implementation(projects.glog)
    implementation(projects.concurrency)
    implementation(projects.task)
    implementation(projects.context)
    implementation(projects.ddmlib)
    implementation(Weisj.darklafCore)
}