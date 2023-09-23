allprojects {
    group = "me.gegenbauer.catspy"
    version = "1.0.0"

    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}