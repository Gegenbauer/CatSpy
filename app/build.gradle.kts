import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version Kotlin.version
    id("org.jetbrains.compose") version Compose.version
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(files("../libs/flatlaf-2.1.jar"))
    implementation(kotlin("reflect"))
    implementation(compose.desktop.currentOs)
    implementation(projects.log)
    implementation(projects.concurrency)
    implementation(projects.databinding)
    implementation(FormDev.flatLaf.group, FormDev.flatLaf.artifact, FormDev.flatLaf.version)
    implementation(Weisj.darklafCore.group, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    implementation(Weisj.darklafVisualPadding.group, Weisj.darklafVisualPadding.artifact, Weisj.darklafVisualPadding.version)
    implementation(Gson.gson.group, Gson.gson.artifact, Gson.gson.version)
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
}

val version = "1.0.0"
val appName = APP_NAME

// TODO 更改应用安装后的图标
compose.desktop {
    application {
        mainClass = "me.gegenbauer.catspy.Main"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            modules += "jdk.unsupported"
            packageName = appName
            packageVersion = version
            group = "me.gegenbauer"

            val iconsRoot = project.file("src/main/resources/appicon/")

            linux {
                iconFile.set(iconsRoot.resolve("icon-linux.png"))
            }
        }
    }
}