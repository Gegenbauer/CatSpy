package me.gegenbauer.catspy.utils.file

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.java.ext.EMPTY_STRING

class JsonFileManager: KeyValuesFileManager() {

    override val fileExtension: String = FILE_EXTENSION

    private val gsonBuilder = GsonBuilder().apply {
        setPrettyPrinting()
    }
    private var cachedGson = gsonBuilder.create()

    /**
     * Serializes the data map to a JSON string
     */
    override fun serialize(data: Map<String, Any?>): String {
        return gsonBuilder.create().toJson(serializeMapToJson(data))
    }

    private fun serializeMapToJson(map: Map<String, Any?>): JsonObject {
        val jsonObject = JsonObject()
        map.forEach { (key, value) ->
            jsonObject.add(key, serializeValue(value))
        }
        return jsonObject
    }

    private fun serializeValue(value: Any?): JsonElement {
        return when (value) {
            is String -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Map<*, *> -> serializeMapToJson(value as Map<String, Any?>)
            is List<*> -> {
                val jsonArray = JsonArray()
                value.forEach { item -> jsonArray.add(serializeValue(item)) }
                jsonArray
            }
            is Set<*> -> {
                val jsonArray = JsonArray()
                value.forEach { item -> jsonArray.add(serializeValue(item)) }
                jsonArray
            }
            else -> {
                val clazz = value?.javaClass?.name ?: EMPTY_STRING
                JsonObject().apply {
                    addProperty(OBJECT_CLAZZ_KEY, clazz)
                    add(OBJECT_VALUE_KEY, gsonBuilder.create().toJsonTree(value))
                }
            }
        }
    }

    override fun deserialize(raw: String): Map<String, Any?> {
        val gson = gsonBuilder.create()
        val jsonObject = gson.fromJson(raw, JsonObject::class.java)
        if (jsonObject == null || jsonObject.isJsonNull) {
            return emptyMap()
        }
        return deserializeJsonObject(jsonObject)
    }

    private fun deserializeJsonObject(jsonObject: JsonObject): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        jsonObject.entrySet().forEach { (key, value) ->
            map[key] = deserializeValue(value)
        }
        return map
    }

    private fun deserializeValue(jsonElement: JsonElement): Any? {
        return when {
            jsonElement.isJsonPrimitive -> {
                val primitive = jsonElement.asJsonPrimitive
                when {
                    primitive.isString -> primitive.asString
                    primitive.isNumber -> primitive.asNumber
                    primitive.isBoolean -> primitive.asBoolean
                    else -> primitive
                }
            }
            jsonElement.isJsonArray -> {
                val jsonArray = jsonElement.asJsonArray
                val list = mutableListOf<Any?>()
                jsonArray.forEach { item -> list.add(deserializeValue(item)) }
                list.filterNotNull()
            }
            else -> {
                val jsonObject = jsonElement.asJsonObject
                val clazz = jsonObject.get(OBJECT_CLAZZ_KEY)?.asString ?: EMPTY_STRING
                if (clazz.isEmpty()) {
                    deserializeJsonObject(jsonObject.getAsJsonObject(OBJECT_VALUE_KEY))
                } else {
                    kotlin.runCatching {
                        gsonBuilder.create().fromJson(jsonObject.get(OBJECT_VALUE_KEY), Class.forName(clazz))
                    }.onFailure {
                        GLog.w(TAG, "[deserializeValue] failed to deserialize object of class $clazz", it)
                    }.getOrNull()
                }
            }
        }
    }

    @Synchronized
    fun registerTypeAdapter(type: Class<*>, typeAdapter: Any) {
        gsonBuilder.registerTypeAdapter(type, typeAdapter)
        cachedGson = gsonBuilder.create()
    }

    companion object {
        const val FILE_EXTENSION = "json"

        private const val TAG = "JsonFileManager"
        private const val OBJECT_CLAZZ_KEY = "clazz"
        private const val OBJECT_VALUE_KEY = "value"
    }
}