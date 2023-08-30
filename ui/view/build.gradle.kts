import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    api(files("libs/flatlaf-2.1.jar"))
    api(files("libs/swingx-1.6.1.jar"))
    implementation(projects.ui.utils)
    implementation(projects.ui.databinding)
    implementation(projects.ui.configuration)
    implementation(projects.glog)
    implementation(projects.ddmlib)
    implementation(projects.resources)
    implementation(projects.context)
    implementation(projects.cache)

    api(TableLayout.groupName, TableLayout.tablelayout.artifact, TableLayout.tablelayout.version)
    api(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    api(FormDev.groupName, FormDev.flatLaf.artifact, FormDev.flatLaf.version)
    api(Weisj.groupName, Weisj.darklafVisualPadding.artifact, Weisj.darklafVisualPadding.version)
    implementation(Gson.groupName, Gson.gson.artifact, Gson.gson.version)
    implementation(Fifesoft.groupName, Fifesoft.autocomplete.artifact, Fifesoft.autocomplete.version)

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