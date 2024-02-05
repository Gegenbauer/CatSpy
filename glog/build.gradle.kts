plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    implementation(projects.file)
    implementation(Slf4j.api)
    implementation(Logback.classic)
}