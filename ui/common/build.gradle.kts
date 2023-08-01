import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    api(files("libs/flatlaf-2.1.jar"))
    api(files("libs/swingx-1.6.1.jar"))
    api(projects.utils)
    api(projects.databinding)
    api(projects.task)
    api(projects.glog)
    api(projects.ddmlib)

    api(Gson.groupName, Gson.gson.artifact, Gson.gson.version)
    api(FormDev.groupName, FormDev.flatLaf.artifact, FormDev.flatLaf.version)
    api(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    api(Weisj.groupName, Weisj.darklafVisualPadding.artifact, Weisj.darklafVisualPadding.version)
    api(Fifesoft.groupName, Fifesoft.autocomplete.artifact, Fifesoft.autocomplete.version)
    api(TableLayout.groupName, TableLayout.tablelayout.artifact, TableLayout.tablelayout.version)

    compileOnly(kotlin("reflect"))
    compileOnly(projects.glog)
    compileOnly(projects.databinding)
    compileOnly(projects.concurrency)
    compileOnly(projects.task)
    compileOnly(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    testImplementation(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    testImplementation(kotlin("test"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}