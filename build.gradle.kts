plugins {
    idea
    id("com.diffplug.spotless")
    id("com.github.vlsi.crlf")
    id("com.github.vlsi.gradle-extensions")
    id("com.github.vlsi.stage-vote-release")
    id("org.ajoberstar.grgit")
    id("net.ltgt.errorprone") apply false
}

allprojects {
    group = "me.gegenbauer.logviewer"
    version = "1.0.0"

    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}