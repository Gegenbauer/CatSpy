plugins {
    kotlin("jvm") version Kotlin.version
    `java-library`
    `module-info-compile`
}

dependencies {
    compileOnly(projects.log)
    compileOnly(projects.concurrency)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}