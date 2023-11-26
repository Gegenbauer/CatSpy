import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.DependencyHandlerScope
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
    val version = "1.1.0"
}

object Weisj {
    val groupName = "com.github.weisj"

    val darklafCore = Dependency(groupName, "darklaf-core", "3.0.2")
    val darklafVisualPadding = Dependency(groupName, "swing-extensions-visual-padding", "0.1.3")
    val darklafExtensitionKotlin = Dependency(groupName, "darklaf-extension-kotlin", "0.3.4")
    val darklafPropertyLoader = Dependency(groupName, "darklaf-property-loader", "3.0.2")
}

object Gson {
    val groupName = "com.google.code.gson"

    val gson = Dependency(groupName, "gson", "2.10.1")
}

object JGoodies {
    val groupName = "com.jgoodies"

    val binding = Dependency(groupName, "jgoodies-binding", "2.13.0")
}

object FormDev {
    val groupName = "com.formdev"

    val flatLaf = Dependency(groupName, "flatlaf", "3.0")
}

object Adam {
    val groupName = "com.malinskiy.adam"

    val adam = Dependency(groupName, "adam", "0.5.0")
}

object Slf4j {
    val groupName = "org.slf4j"

    val simple = Dependency(groupName, "slf4j-simple", "1.7.32")
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
    const val flatlaf = "libs/flatlaf-2.1.jar"
    const val swingx = "libs/swingx-1.6.1.jar"
}