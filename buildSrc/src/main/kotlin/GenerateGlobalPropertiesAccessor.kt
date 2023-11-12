import org.gradle.api.Project
import org.gradle.kotlin.dsl.extra
import java.util.*

fun generateGlobalPropertiesAccessor(project: Project): String {
    val properties = project.extra.properties.filter { it.key.startsWith("app.") }
    val builder = StringBuilder()

    builder.appendLine("package me.gegenbauer.catspy.platform;")
    builder.appendLine()

    builder.appendLine("@javax.annotation.Generated(value = {\"GenerateGlobalPropertiesAccessor\"})")
    builder.append("public class GlobalProperties {\n")
    for (key in properties.keys.sorted()) {
        val value = properties[key]
        if (value is String) {
            builder.appendLine()
            builder.appendLine("${1.indent} public static final String ${formalizePropertyName(key)} = \"$value\";")
        }
    }
    builder.append("}")
    return builder.toString()
}

/**
 * app.name -> APP_NAME
 */
private fun formalizePropertyName(raw: String): String {
    val parts = raw.split(".")
    val builder = StringBuilder()
    for (part in parts) {
        builder.append(part.uppercase(Locale.getDefault()))
        builder.append("_")
    }
    builder.deleteCharAt(builder.length - 1)
    return builder.toString()
}