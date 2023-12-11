plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.ui.utils)
    implementation(projects.ui.databinding)
    implementation(projects.concurrency)
    implementation(projects.resources.iconset)
    implementation(projects.resources.strings)
    implementation(projects.resources.common)
    implementation(projects.platform)
    implementation(projects.ddmlib)
    implementation(projects.task)
    implementation(projects.glog)
    implementation(projects.javaext)
    implementation(projects.cache)
    implementation(projects.file)
    implementation(Weisj.darklafCore)
    implementation(FormDev.flatLaf)
    implementation(FormDev.extra)
    implementation(FormDev.intelliJTheme)
    api(files(FileDependency.innerFont))
    api(files(FileDependency.jetbrainsMonoFont))
    api(files(FileDependency.robotoFont))
    api(files(FileDependency.robotoMonoFont))
    api(files(FileDependency.jFontChooser))
}

