object Kotlin {
    val groupName = "org.jetbrains.kotlinx"
    val version = "1.6.10"

    val coroutineVersion = "1.5.2"
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

data class Dependency(
    val group: String,
    val artifact: String,
    val version: String
) {
    override fun toString(): String {
        return "$group:$artifact:$version"
    }
}