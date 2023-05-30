plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    compileOnly(projects.log)
    compileOnly(projects.concurrency)
    compileOnly(projects.concurrency)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}