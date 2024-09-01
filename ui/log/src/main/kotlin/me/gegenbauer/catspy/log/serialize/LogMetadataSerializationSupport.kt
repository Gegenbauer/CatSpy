package me.gegenbauer.catspy.log.serialize

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.parse.LogParser
import me.gegenbauer.catspy.utils.IdGenerator
import me.gegenbauer.catspy.utils.file.Serializer
import me.gegenbauer.catspy.utils.ui.hexToColor
import me.gegenbauer.catspy.utils.ui.toHex
import me.gegenbauer.catspy.view.color.DarkThemeAwareColor
import java.lang.reflect.Type

class LogMetadataSerializer : Serializer<LogMetadata, String> {

    override fun serialize(target: LogMetadata): String {
        return logMetadataModelSerializer.serialize(target.toLogMetadataModel())
    }

    override fun deserialize(serialized: String): LogMetadata {
        return logMetadataModelSerializer.deserialize(serialized).toLogMetadata()
    }

    companion object {
        private val logMetadataModelSerializer = LogMetadataModelSerializer()
    }
}

private class LogParserAdapter : JsonDeserializer<LogParser>, JsonSerializer<LogParser> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LogParser {
        return parserSerializer.deserialize(json)
    }

    override fun serialize(src: LogParser, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return parserSerializer.serialize(src as SerializableLogParser)
    }

    companion object {
        private val parserSerializer = LogParserSerializer()
    }
}

private class DarkThemeAwareColorAdapter : JsonDeserializer<DarkThemeAwareColor>, JsonSerializer<DarkThemeAwareColor> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): DarkThemeAwareColor {
        val jsonObject = json?.asJsonObject
        val color = jsonObject?.get(KEY_COLOR)?.asString ?: EMPTY_STRING
        val darkThemeColor = jsonObject?.get(KEY_DARK_THEME_COLOR)?.asString ?: EMPTY_STRING
        return DarkThemeAwareColor(color.hexToColor(), darkThemeColor.hexToColor())
    }

    override fun serialize(src: DarkThemeAwareColor, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty(KEY_COLOR, src.dayColor.toHex())
        jsonObject.addProperty(KEY_DARK_THEME_COLOR, src.nightColor.toHex())
        return jsonObject
    }

    companion object {
        private const val KEY_COLOR = "color"
        private const val KEY_DARK_THEME_COLOR = "darkThemeColor"
    }
}

class LogMetadataModelSerializer : Serializer<LogMetadataModel, String> {

    override fun serialize(target: LogMetadataModel): String {
        return gson.toJson(target)
    }

    override fun deserialize(serialized: String): LogMetadataModel {
        val logMetadataModel = gson.fromJson(serialized, LogMetadataModel::class.java)
        val columns = logMetadataModel.columns.map {
            val columnId = IdGenerator.generateId()
            it.copy(
                id = columnId,
                uiConf = it.uiConf.copy(filter = it.uiConf.filter.copy(columnId = columnId, columnName = it.name))
            )
        }
        return logMetadataModel.copy(columns = columns)
    }

    companion object {
        private val gson = GsonBuilder()
            .registerTypeAdapter(LogParser::class.java, LogParserAdapter())
            .registerTypeAdapter(DarkThemeAwareColor::class.java, DarkThemeAwareColorAdapter())
            .setPrettyPrinting()
            .create()
    }
}