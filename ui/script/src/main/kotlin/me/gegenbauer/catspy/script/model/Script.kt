package me.gegenbauer.catspy.script.model

import me.gegenbauer.catspy.script.model.ScriptCategory.Companion.android
import me.gegenbauer.catspy.script.model.ScriptType.Companion.adb

data class Script(
    val name: String,
    val type: ScriptType,
    val sourceCode: String,
    val description: String = "",
) {
    fun getCommand(): String {
        return "${type.prefix} $sourceCode"
    }
}

data class ScriptType(
    val name: String,
    val category: ScriptCategory,
    val prefix: String,
) {
    companion object {
        val adb = ScriptType("adb", android, "adb shell")
    }
}

val scriptCategories = mutableListOf(
    android,
)
val scriptTypes = mutableListOf(
    adb
)

data class ScriptCategory(
    val name: String,
    val description: String = "",
    val parentCategory: ScriptCategory? = null,
) {
    companion object {
        val android = ScriptCategory("Android", "Scripts for android development")
    }
}