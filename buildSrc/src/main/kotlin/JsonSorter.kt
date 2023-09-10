import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import java.io.File

fun sortProperties(jsonFile: File): JsonObject {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val jsonObject = gson.fromJson(jsonFile.reader(), JsonObject::class.java)
    return sortProperties(jsonObject)
}

fun JsonObject.toPrettyString(): String {
    return GsonBuilder().setPrettyPrinting().create().toJson(this)
}

fun sortProperties(json: JsonObject): JsonObject {
    val result = JsonObject()
    val keys = json.keySet().sorted()
    for (key in keys) {
        val value = json[key]
        if (value.isJsonObject) {
            result.add(key, sortProperties(value.asJsonObject))
        } else {
            result.add(key, value)
        }
    }
    return result
}