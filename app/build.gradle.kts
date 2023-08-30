import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version Kotlin.version
    id("org.jetbrains.compose") version Compose.version
    id("io.gitlab.arturbosch.detekt") version("1.21.0")
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(projects.ui)
    implementation(projects.glog)
    implementation(projects.platform)
    implementation(projects.concurrency)
    implementation(projects.task)
    implementation(projects.ddmlib)
    implementation(projects.cache)
    implementation(projects.context)
    implementation(projects.resources)

    testImplementation(kotlin("test"))
    testImplementation(projects.ui)
    testImplementation(Mockk.groupName, Mockk.mockk.artifact, Mockk.mockk.version)
}

tasks.test {
    useJUnitPlatform()
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
            jvmArgs += "--add-exports=java.desktop/sun.awt=ALL-UNNAMED"

            val iconsRoot = project.file("src/main/resources/appicon/")

            linux {
                iconFile.set(iconsRoot.resolve("icon-linux.png"))
            }
        }
    }
}