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
    implementation(files("../libs/swingx-1.6.1.jar"))
    implementation(kotlin("reflect"))

    implementation(compose.desktop.currentOs)
    implementation(projects.log)
    implementation(projects.concurrency)
    implementation(projects.databinding)
    implementation(projects.task)
    implementation(projects.ddmlib)
    implementation(projects.filter)
    implementation(projects.utils)

    implementation(FormDev.groupName, FormDev.flatLaf.artifact, FormDev.flatLaf.version)
    implementation(Weisj.groupName, Weisj.darklafCore.artifact, Weisj.darklafCore.version)
    implementation(Weisj.groupName, Weisj.darklafVisualPadding.artifact, Weisj.darklafVisualPadding.version)
    implementation(Gson.groupName, Gson.gson.artifact, Gson.gson.version)
    implementation("com.fifesoft:autocomplete:3.3.1")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
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
        mainClass = "me.gegenbauer.catspy.Application"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            modules += "jdk.unsupported"
            modules += "jdk.management"
            packageName = appName
            packageVersion = version
            group = "me.gegenbauer"
            jvmArgs += "-Xmx300m"

            val iconsRoot = project.file("src/main/resources/appicon/")

            linux {
                iconFile.set(iconsRoot.resolve("icon-linux.png"))
            }
        }
    }
}