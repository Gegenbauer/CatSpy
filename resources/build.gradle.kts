plugins {
    kotlin("jvm")
    `java-library`
    `module-info-compile`
}

dependencies {
    api(projects.resources.iconset)
    api(projects.resources.strings)
    api(projects.resources.common)
}
