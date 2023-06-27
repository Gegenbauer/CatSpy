enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "CatSpy"
include(
    "app",
    "databinding",
    "glog",
    "concurrency",
    "ddmlib",
    "task",
    "filter",
    "utils",
    "ui",
    "ui:log",
    "ui:script",
    "ui:common",
)

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
    plugins {
        fun idv(id: String, key: String = id) = id(id) version extra["$key.version"].toString()
        idv("com.diffplug.spotless")
        idv("com.github.vlsi.crlf", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.gradle-extensions", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.license-gather", "com.github.vlsi.vlsi-release-plugins")
        idv("com.github.vlsi.stage-vote-release", "com.github.vlsi.vlsi-release-plugins")
        idv("org.ajoberstar.grgit")
        idv("net.ltgt.errorprone")
    }
}
