import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    compileOnly(projects.concurrency)
    compileOnly(kotlin("reflect"))
    compileOnly(projects.log)
    compileOnly(Weisj.darklafCore.group, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    testImplementation(projects.app)
    testImplementation(projects.concurrency)
    testImplementation(kotlin("reflect"))
    testImplementation(projects.log)
    testImplementation(Weisj.darklafCore.group, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    testImplementation(JGoodies.binding.group, JGoodies.binding.artifact, JGoodies.binding.version)
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