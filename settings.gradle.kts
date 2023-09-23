pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.6.10" apply false
        id("com.squareup.sqldelight") version "1.5.4" apply false
        id("org.jetbrains.compose") version "1.1.0" apply false
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "CatSpy"

include(
    "app",
    "glog",
    "file",
    "platform",
    "javaext",
    "concurrency",
    "ddmlib",
    "task",
    "cache",
    "context",
    "ui",
    "ui:log",
    "ui:script",
    "ui:view",
    "ui:databinding",
    "ui:render",
    "ui:filter",
    "ui:utils",
    "ui:configuration",
    "ui:database",
    "resources",
    "resources:iconset",
    "resources:strings",
    "resources:common",
)
