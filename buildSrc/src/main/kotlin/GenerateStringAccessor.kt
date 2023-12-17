import com.google.gson.JsonObject
import java.io.File

fun createStringAccessor(jsonFile: File, packageName: String, className: String): String {
    val gson = com.google.gson.Gson()
    val jsonObject = gson.fromJson(jsonFile.reader(), JsonObject::class.java)

    val result = StringBuilder()

    result.appendLine("package $packageName;")
    result.appendLine()

    fun String.toClassName(): String {
        return this.replaceFirstChar { it.uppercase() }
    }

    fun parseJson(name: String, json: JsonObject, depth: Int) {
        result.appendLine("${depth.indent}@javax.annotation.Generated(value = {\"GenerateStringAccessor\"})")
        val staticModifier = if (depth != 0) " static" else ""
        result.appendLine("${depth.indent}public${staticModifier} class ${name.toClassName()} {")
        val newDepth = depth + 1
        for (entry in json.entrySet()) {
            val key = entry.key
            val value = entry.value

            if (value.isJsonObject) {
                result.appendLine("${newDepth.indent}public ${key.toClassName()} $key = new ${key.toClassName()}();")
                parseJson(key, value.asJsonObject, newDepth)
            } else {
                result.appendLine(
                    "${newDepth.indent}public final String $key = " +
                            "\"${value.asString.replace("\n", "\\n")}\";"
                )
            }
        }

        result.appendLine("${(newDepth - 1).indent}}")
        result.appendLine()
    }

    parseJson(className, jsonObject, 0)

    return result.toString()
}