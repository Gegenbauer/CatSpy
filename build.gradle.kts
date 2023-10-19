// build.gradle.kts 文件
plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    kotlin("jvm") apply false
    kotlin("multiplatform") apply false
    id("org.jetbrains.compose") apply false
}

allprojects {
    group = "me.gegenbauer.catspy"
    version = "1.0.0"

    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}