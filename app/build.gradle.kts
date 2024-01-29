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
            copyright = "© 2023 Gegenbauer. All rights reserved."

            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            modules += listOf("jdk.unsupported", "jdk.management")
            jvmArgs += "--add-exports=java.desktop/sun.awt=ALL-UNNAMED"

            packageName = project.extra["app.name"].toString()
            group = "me.gegenbauer"
            description = "A simple tool to browse your log files or Android device logs " +
                    "and can control your device with given adb commands."

            val iconsRoot = project.file("src/main/resources/appicon/")

            linux {
                iconFile.set(iconsRoot.resolve("icon-linux.png"))
                appRelease = project.extra["app.version.name"].toString()
                debPackageVersion = project.extra["app.version.name"].toString()
            }

            windows {
                dirChooser = true
                upgradeUuid = "eff1902c-4e55-11ee-be56-0242ac120002"
                msiPackageVersion = project.extra["app.version.name"].toString()
            }

            macOS {
                iconFile.set(iconsRoot.resolve("icon-linux.png"))
                appCategory = "public.app-category.developer-tools"
                packageName = project.extra["app.name"].toString()
                dmgPackageVersion = project.extra["app.version.name"].toString()
                dmgPackageBuildVersion = project.extra["app.version.code"].toString()
            }
        }
    }
}