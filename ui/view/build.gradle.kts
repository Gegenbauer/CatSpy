import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    api(files(FileDependency.flatlaf))
    api(files(FileDependency.swingx))
    implementation(projects.ui.utils)
    implementation(projects.ui.databinding)
    implementation(projects.ui.configuration)
    implementation(projects.ui.filter)
    implementation(projects.glog)
    implementation(projects.ddmlib)
    implementation(projects.resources)
    implementation(projects.context)
    implementation(projects.cache)
    implementation(projects.platform)
    implementation(projects.concurrency)
    implementation(projects.network)

    api(TableLayout.tablelayout)
    api(Weisj.darklafCore)
    api(FormDev.flatLaf)
    api(Weisj.darklafVisualPadding)
    implementation(Gson.gson)
    implementation(Fifesoft.autocomplete)

    testImplementation(Weisj.darklafCore)
    testImplementation(kotlinTestApi())
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}