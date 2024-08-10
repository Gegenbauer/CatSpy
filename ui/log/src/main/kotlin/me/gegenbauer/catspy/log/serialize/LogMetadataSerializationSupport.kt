package me.gegenbauer.catspy.log.serialize

import com.google.gson.*
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.parse.LogParser
import me.gegenbauer.catspy.utils.IdGenerator
import me.gegenbauer.catspy.utils.file.Serializer
import me.gegenbauer.catspy.view.color.DarkThemeAwareColor
import java.awt.Color
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
        val color = jsonObject?.get(KEY_COLOR)?.toInt() ?: 0
        val darkThemeColor = jsonObject?.get(KEY_DARK_THEME_COLOR)?.toInt() ?: 0
        return DarkThemeAwareColor(color.toColor(), darkThemeColor.toColor())
    }

    private fun JsonElement.toInt(): Int {
        val formattedString = asString.lowercase()
        return if (formattedString.startsWith(HEX_PREFIX)) {
            Integer.parseInt(formattedString.substring(2), 16)
        } else {
            Integer.parseInt(formattedString)
        }
    }

    private fun Color.toSerializedString(): String {
        return HEX_PREFIX + toInt().toString(16)
    }

    override fun serialize(src: DarkThemeAwareColor, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val jsonObject = JsonObject()
        jsonObject.addProperty(KEY_COLOR, src.dayColor.toSerializedString())
        jsonObject.addProperty(KEY_DARK_THEME_COLOR, src.nightColor.toSerializedString())
        return jsonObject
    }

    private fun Color.toInt(): Int {
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }

    private fun Int.toColor(): Color {
        return Color(this, true)
    }

    companion object {
        private const val KEY_COLOR = "color"
        private const val KEY_DARK_THEME_COLOR = "darkThemeColor"
        private const val HEX_PREFIX = "0x"
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