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
    implementation(projects.context)
    implementation(Weisj.darklafCore)
    implementation(FormDev.flatLaf)
    implementation(FormDev.extra)
    implementation(FormDev.intelliJTheme)
    implementation(FormDev.fontInter)
    implementation(FormDev.fontJetbrainsMono)
    implementation(FormDev.fontRobotoMono)
    implementation(FormDev.fontRoboto)
    implementation(TableLayout.tablelayout)
    api(files(FileDependency.jFontChooser))
}

