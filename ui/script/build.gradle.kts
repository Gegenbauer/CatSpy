import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    api(projects.ui.common)
    compileOnly(projects.glog)
    compileOnly(kotlin("reflect"))
    compileOnly(projects.concurrency)
    compileOnly(projects.task)
    compileOnly(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    testImplementation(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
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