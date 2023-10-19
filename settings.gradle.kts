pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    plugins {
        val kotlinVersion = extra["kotlinVersion"] as String
        val composeVersion = extra["composeVersion"] as String

        kotlin("jvm").version(kotlinVersion)
        kotlin("multiplatform").version(kotlinVersion)
        id("org.jetbrains.compose").version(composeVersion)
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
    "resources",
    "resources:iconset",
    "resources:strings",
    "resources:common",
)
