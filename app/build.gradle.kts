import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id(Compose.groupName)
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
    implementation(projects.javaext)
    implementation(projects.file)
    implementation(projects.network)

    testImplementation(kotlinTestApi())
    testImplementation(projects.ui)
    testImplementation(projects.concurrency)
    testImplementation(Mockk.mockk)
}

tasks.test {
    useJUnitPlatform()
}

// TODO 更改应用安装后的图标
compose.desktop {
    application {
        mainClass = "me.gegenbauer.catspy.Application"

        nativeDistributions {
            licenseFile.set(rootProject.file("LICENSE"))

            val author = project.extra["app.author"].toString().capitalized()
            copyright = "© 2023 $author. All rights reserved."
            vendor = author.capitalized()

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            modules("jdk.unsupported", "jdk.management", "java.instrument", "java.management", "jdk.management.agent")
            jvmArgs += "--add-exports=java.desktop/sun.awt=ALL-UNNAMED"

            packageName = project.extra["app.name"].toString()
            group = "me.gegenbauer"
            description = "A simple tool to browse your log files or Android device logs " +
                    "and can control your device with given adb commands."

            val iconsRoot = project.file("src/main/resources/appicon/")

            linux {
                iconFile.set(iconsRoot.resolve("icon.png"))
                appRelease = project.extra["app.version.name"].toString()

                packageVersion = project.extra["app.version.name"].toString()
            }

            windows {
                dirChooser = true
                upgradeUuid = "eff1902c-4e55-11ee-be56-0242ac120002"

                packageVersion = project.extra["app.version.name"].toString()
            }

            macOS {
                iconFile.set(iconsRoot.resolve("icon.icns"))
                appCategory = "public.app-category.developer-tools"
                bundleID = project.extra["app.id"].toString()
                dockName = project.extra["app.name"].toString()

                packageVersion = project.extra["app.version.name"].toString()
                packageBuildVersion = project.extra["app.version.name"].toString()
            }
        }
    }
}