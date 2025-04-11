plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    api(files(FileDependency.swingx))
    api(files(FileDependency.toast))
    implementation(projects.ui.utils)
    implementation(projects.ui.databinding)
    implementation(projects.ui.configuration)
    implementation(projects.ui.filter)
    implementation(projects.ui.render)
    implementation(projects.glog)
    implementation(projects.ddmlib)
    implementation(projects.resources)
    implementation(projects.javaext)
    implementation(projects.context)
    implementation(projects.cache)
    implementation(projects.platform)
    implementation(projects.concurrency)
    implementation(projects.network)
    implementation(projects.file)

    api(TableLayout.tablelayout)
    api(Weisj.darklafCore)
    api(FormDev.flatLaf)
    api(FormDev.extra)
    api(Weisj.darklafVisualPadding)
    api(AlexandriaSoftware.jInputValidator)
    implementation(Fifesoft.autocomplete)
    implementation(MigLayout.swing)

    testImplementation(Weisj.darklafCore)
    testImplementation(kotlinTestApi())
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}