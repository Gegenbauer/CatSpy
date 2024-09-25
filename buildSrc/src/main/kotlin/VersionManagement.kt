import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.kotlin

object Kotlin {
    val groupName = "org.jetbrains.kotlinx"
    val version = "1.8.22"

    val coroutineVersion = "1.7.1"
    val coroutineCore = Dependency(groupName, "kotlinx-coroutines-core", coroutineVersion)
    val coroutineSwing = Dependency(groupName, "kotlinx-coroutines-swing", coroutineVersion)
}

object Compose {
    val groupName = "org.jetbrains.compose"
    val version = "1.6.11"

    val runtime = Dependency("org.jetbrains.compose.runtime", "runtime", version)
}

object Weisj {
    val groupName = "com.github.weisj"

    val darklafCore = Dependency(groupName, "darklaf-core", "3.0.2")
    val darklafVisualPadding = Dependency(groupName, "swing-extensions-visual-padding", "0.1.3")
    val darklafExtensitionKotlin = Dependency(groupName, "darklaf-extension-kotlin", "0.3.4")
    val darklafPropertyLoader = Dependency(groupName, "darklaf-property-loader", "3.0.2")
    val swingExtVisualPadding = Dependency(groupName, "swing-extensions-visual-padding", "0.1.3")
    val jsvg = Dependency(groupName, "jsvg", "1.4.0")
}

object Gson {
    val groupName = "com.google.code.gson"

    val gson = Dependency(groupName, "gson", "2.10.1")
}

object JGoodies {
    val groupName = "com.jgoodies"

    val binding = Dependency(groupName, "jgoodies-binding", "2.13.0")
}

/**
 * https://github.com/JFormDesigner/FlatLaf
 */
object FormDev {
    val groupName = "com.formdev"
    val version = "3.4.1"
    val interFontVersion = "4.0"
    val jetbrainsMonoFontVersion = "2.304"
    val robotoMonoFontVersion = "3.000"
    val robotoFontVersion = "2.137"

    val flatLaf = Dependency(groupName, "flatlaf", version)
    val extra = Dependency(groupName, "flatlaf-extras", version)
    val intelliJTheme = Dependency(groupName, "flatlaf-intellij-themes", version)
    val fontInter = Dependency(groupName, "flatlaf-fonts-inter", interFontVersion)
    val fontJetbrainsMono = Dependency(groupName, "flatlaf-fonts-jetbrains-mono", jetbrainsMonoFontVersion)
    val fontRobotoMono = Dependency(groupName, "flatlaf-fonts-roboto-mono", robotoMonoFontVersion)
    val fontRoboto = Dependency(groupName, "flatlaf-fonts-roboto", robotoFontVersion)
}

object Adam {
    val groupName = "com.malinskiy.adam"

    val adam = Dependency(groupName, "adam", "0.5.3")
}

object Slf4j {
    val groupName = "org.slf4j"

    val simple = Dependency(groupName, "slf4j-simple", "2.0.0")
    val api = Dependency(groupName, "slf4j-api", "2.0.0")
}

object Logback {
    val groupName = "ch.qos.logback"

    val classic = Dependency(groupName, "logback-classic", "1.4.14")
}

object ApacheLogging {
    val groupName = "org.apache.logging.log4j"
    val version = "2.17.1"

    val log4jCore = Dependency(groupName, "log4j-core", version)
    val log4jApi = Dependency(groupName, "log4j-api", version)
}

object Fifesoft {
    val groupName = "com.fifesoft"

    val autocomplete = Dependency(groupName, "autocomplete", "3.3.1")
}

object Mockk {
    val groupName = "io.mockk"

    val mockk = Dependency(groupName, "mockk", "1.12.5")
}

object TableLayout {
    val groupName = "tablelayout"

    val tablelayout = Dependency(groupName, "TableLayout", "20050920")
}

object AndroidDdm {
    val groupName = "com.android.tools.ddms"

    val ddmlib = Dependency(groupName, "ddmlib", "26.1.3")
}

object JavaXAnno {
    val groupName = "javax.annotation"

    val annotationApi = Dependency(groupName, "javax.annotation-api", "1.3.2")
}

object Squareup {
    val groupName = "com.squareup.okhttp3"
    val version = "4.11.0"

    val okhttp = Dependency(groupName, "okhttp", version)
    val okhttpLoggingInterceptor = Dependency(groupName, "logging-interceptor", version)
    val okio = Dependency("com.squareup.okio", "okio", "3.9.1")
}

object MigLayout {
    val groupName = "com.miglayout"

    val swing = Dependency(groupName, "miglayout-swing", "5.3")
}

object AlexandriaSoftware {
    val groupName = "com.alexandriasoftware.swing"

    val jInputValidator = Dependency(groupName, "jinputvalidator", "0.9.0")
}

object Oshi {
    private const val GROUP_NAME = "com.github.oshi"
    private const val VERSION = "6.6.3"

    val oshiCore = Dependency(GROUP_NAME, "oshi-core", VERSION)
}

data class Dependency(
    val group: String,
    val artifact: String,
    val version: String
) {
    override fun toString(): String {
        return "$group:$artifact:$version"
    }
}

fun DependencyHandler.implementation(dependency: Dependency) {
    add("implementation", dependency.toString())
}

fun DependencyHandler.testImplementation(dependency: Dependency) {
    add("testImplementation", dependency.toString())
}

fun DependencyHandler.api(dependency: Dependency) {
    add("api", dependency.toString())
}

fun DependencyHandler.kotlinTestApi(): Any {
    return kotlin("test")
}

fun DependencyHandler.kotlinReflectApi(): Any {
    return kotlin("reflect")
}

object FileDependency {
    const val swingx = "libs/swingx-1.6.1.jar"
    const val jFontChooser = "libs/jfontchooser-1.0.5.jar"
    const val toast = "libs/swing-toast-notifications-1.0.3.jar"
}