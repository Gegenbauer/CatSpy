import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.0"
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(files("libs/flatlaf-2.1.jar"))
    implementation("com.github.weisj:darklaf-core:3.0.2")
    implementation("com.github.weisj:darklaf-extensions-kotlin:0.1.0")
    implementation("com.github.weisj:darklaf-extensions-rsyntaxarea:0.1.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(kotlin("reflect"))
    implementation("com.formdev:flatlaf:3.0")
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

// TODO 更改应用安装后的图标
compose.desktop {
    application {
        mainClass = "me.gegenbauer.logviewer.Main"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            modules += "jdk.unsupported"
            packageName = "LogViewer"
            packageVersion = "1.0.0"
            group = "me.gegenbauer"
        }
    }
}
