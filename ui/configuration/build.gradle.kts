plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.ui.utils)
    implementation(projects.concurrency)
    implementation(projects.resources.iconset)
    implementation(projects.resources.strings)
    implementation(projects.platform)
    implementation(projects.ddmlib)
    implementation(projects.task)
    implementation(projects.glog)
    implementation(projects.javaext)
    implementation(projects.cache)
    implementation(Gson.gson)
    implementation(Weisj.darklafCore)
}

