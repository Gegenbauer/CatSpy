plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    api(projects.concurrency)
    implementation(projects.log)
    implementation(Weisj.darklafCore.group, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    implementation(JGoodies.binding.group, JGoodies.binding.artifact, JGoodies.binding.version)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}