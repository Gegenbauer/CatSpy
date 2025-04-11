package me.gegenbauer.catspy.log.serialize

import com.google.gson.*
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.java.ext.SPACE_STRING
import me.gegenbauer.catspy.java.ext.WORD_REGEX
import me.gegenbauer.catspy.log.Log
import me.gegenbauer.catspy.log.parse.LogParser
import me.gegenbauer.catspy.log.parse.ParserManager
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.file.Serializer
import java.lang.reflect.Type
import java.util.*

class LogParserSerializer : Serializer<SerializableLogParser, JsonElement> {

    override fun deserialize(serialized: JsonElement): SerializableLogParser {
        return gson.fromJson(serialized, SerializableLogParser::class.java)
    }

    override fun serialize(target: SerializableLogParser): JsonElement {
        return gson.toJsonTree(target)
    }

    class PreProcessOpAdapter : JsonDeserializer<SplitToPartsOp>, JsonSerializer<SplitToPartsOp> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): SplitToPartsOp {
            val jsonObject = json.asJsonObject
            val opType = jsonObject[JSON_KEY_TYPE].asInt
            val opClass = splitToPartsOps[opType] ?: throw IllegalArgumentException("Unknown op type: $opType")
            return gson.fromJson(json, opClass)
        }

        override fun serialize(src: SplitToPartsOp, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            val jsonObject = gson.toJsonTree(src).asJsonObject
            jsonObject.addProperty(JSON_KEY_TYPE, src.type)
            return jsonObject
        }
    }

    class PreProcessPostOpAdapter : JsonDeserializer<SplitPostProcessOp>, JsonSerializer<SplitPostProcessOp> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): SplitPostProcessOp {
            val jsonObject = json.asJsonObject
            val opType = jsonObject[JSON_KEY_TYPE].asInt
            val opClass = preProcessPostOps[opType] ?: throw IllegalArgumentException("Unknown op type: $opType")
            return gson.fromJson(json, opClass)
        }

        override fun serialize(
            src: SplitPostProcessOp,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val jsonObject = gson.toJsonTree(src).asJsonObject
            jsonObject.addProperty(JSON_KEY_TYPE, src.type)
            return jsonObject
        }
    }

    class ExistedParserOpAdapter : JsonDeserializer<ExistedParserOp>, JsonSerializer<ExistedParserOp> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ExistedParserOp {
            val jsonObject = json.asJsonObject
            val clazz = jsonObject["clazz"].asString
            val jarPath = jsonObject["jarPath"].asString
            val isBuiltIn = jsonObject["isBuiltIn"].asBoolean
            return ExistedParserOp(clazz, jarPath, isBuiltIn)
        }

        override fun serialize(src: ExistedParserOp, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            val jsonObject = JsonObject()
            jsonObject.addProperty("clazz", src.clazz)
            jsonObject.addProperty("jarPath", src.jarPath)
            jsonObject.addProperty("isBuiltIn", src.isBuiltIn)
            jsonObject.addProperty(JSON_KEY_TYPE, src.type)
            return jsonObject
        }
    }

    class TrimOpAdapter : JsonDeserializer<TrimOp>, JsonSerializer<TrimOp> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): TrimOp {
            val jsonObject = json.asJsonObject
            val opType = jsonObject[JSON_KEY_TYPE].asInt
            val opClass = parseOps[opType] ?: throw IllegalArgumentException("Unknown op type: $opType")
            return gson.fromJson(json, opClass)
        }

        override fun serialize(src: TrimOp, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            val jsonObject = gson.toJsonTree(src).asJsonObject
            jsonObject.addProperty(JSON_KEY_TYPE, src.type)
            return jsonObject
        }
    }

    companion object {
        private const val JSON_KEY_TYPE = "type"

        private val splitToPartsOps = mutableMapOf<Int, Class<out SplitToPartsOp>>()
        private val preProcessPostOps = mutableMapOf<Int, Class<out SplitPostProcessOp>>()
        private val parseOps = mutableMapOf<Int, Class<out TrimOp>>()

        private val gson = GsonBuilder()
            .registerTypeAdapter(SplitToPartsOp::class.java, PreProcessOpAdapter())
            .registerTypeAdapter(SplitPostProcessOp::class.java, PreProcessPostOpAdapter())
            .registerTypeAdapter(TrimOp::class.java, TrimOpAdapter())
            .registerTypeAdapter(ExistedParserOp::class.java, ExistedParserOpAdapter())
            .create()

        init {
            splitToPartsOps[OP_SPLIT_BY_WORD_SEPARATOR] = SplitByWordSeparatorOp::class.java
            splitToPartsOps[OP_EMPTY_SPLIT_TO_PARTS] = EmptySplitToPartsOp::class.java
            splitToPartsOps[OP_EXISTED_PARSER] = ExistedParserOp::class.java
            preProcessPostOps[OP_SPLIT_PART_WITH_CHAR] = SplitPartWithCharOp::class.java
            preProcessPostOps[OP_MERGE_NEARBY_PARTS] = MergeNearbyPartsOp::class.java
            preProcessPostOps[OP_REMOVE_BLANK_PART] = RemoveBlankPartOp::class.java
            preProcessPostOps[OP_MERGE_UNTIL_CHAR] = MergeUntilCharOp::class.java
            parseOps[OP_TRIM_WITH_CHAR] = TrimWithCharOp::class.java
            parseOps[OP_TRIM_WITH_INDEX] = TrimWithIndexOp::class.java
        }
    }
}

class SerializableLogParser(
    val splitToPartsOp: SplitToPartsOp,
    val trimOps: List<TrimOp>,
) : LogParser {

    @Transient
    private lateinit var levelKeywords: Set<String>

    @Transient
    private lateinit var parseResult: Array<String>

    @Transient
    private lateinit var defaultParseResult: Array<String>

    fun setLogMetadata(logMetadataModel: LogMetadataModel) {
        val levelKeywords = logMetadataModel.levels.map { it.level.keyword }.toSet()
        val columnCount = logMetadataModel.columns.filter { it.isParsed }.size
        this.levelKeywords = levelKeywords
        parseResult = Array(columnCount) { EMPTY_STRING }
        defaultParseResult = Array(columnCount) { EMPTY_STRING }
    }

    override fun parse(
        line: String,
        parseMetadata: LogParser.ParseMetadata
    ): List<String> {
        resetCache()
        val splitResult = splitToPartsOp.process(sequenceOf(line), parseMetadata)
        if (parseMetadata.levelColumn > 0) defaultParseResult[parseMetadata.levelColumn] = parseMetadata.defaultLevelTag
        defaultParseResult[defaultParseResult.size - 1] = line

        val parsedResult = applyTrimOps(splitResult, defaultParseResult, parseMetadata)
        val isValidLog = (parseMetadata.levelColumn > 0)
                && (parsedResult.size > parseMetadata.levelColumn)
                && levelKeywords.contains(parsedResult[parseMetadata.levelColumn])
        if (isValidLog) {
            return parsedResult
        }
        return defaultParseResult.toList()
    }

    private fun resetCache() {
        parseResult.fill(EMPTY_STRING)
        defaultParseResult.fill(EMPTY_STRING)
    }

    private fun applyTrimOps(
        lastResult: Sequence<String>,
        default: Array<String>,
        parseMetadata: LogParser.ParseMetadata
    ): List<String> {
        return kotlin.runCatching {
            if (trimOps.isEmpty()) {
                lastResult
            } else {
                trimOps.fold(lastResult) { parts, op ->
                    op.process(parts, parseMetadata)
                }
            }.toList()
        }.getOrDefault(default.toList())
    }

    override fun equals(other: Any?): Boolean {
        return other is SerializableLogParser
                && other.splitToPartsOp == splitToPartsOp
                && other.trimOps == trimOps
    }

    override fun hashCode(): Int {
        return Objects.hash(javaClass.name, splitToPartsOp, trimOps)
    }
}

interface ParseOp {
    val name: String

    val description: String

    val type: Int

    fun process(
        parts: Sequence<String>,
        parseMetadata: LogParser.ParseMetadata
    ): Sequence<String>
}

/**
 * Preprocessing operation, which refers to splitting the original log line into
 * multiple parts through extraction or splitting.
 * The final result is a sequence of strings, each representing a part,
 * which can also be considered as a column in a table.
 */
interface SplitToPartsOp : ParseOp {
    val splitPostProcessOps: List<SplitPostProcessOp>
}

/**
 * 后处理操作，指对分割后的部分进行处理，对每个部分进一步分割，或者合并相邻部分，或者移除空白部分等
 */
interface SplitPostProcessOp : ParseOp

private const val TYPE_EMPTY = -1

private const val OP_EMPTY_SPLIT_TO_PARTS = 0
private const val OP_SPLIT_BY_WORD_SEPARATOR = 1
private const val OP_EXISTED_PARSER = 2

private const val OP_SPLIT_PART_WITH_CHAR = 10
private const val OP_MERGE_NEARBY_PARTS = 11
private const val OP_REMOVE_BLANK_PART = 12
private const val OP_MERGE_UNTIL_CHAR = 13

private const val OP_TRIM_WITH_CHAR = 100
private const val OP_TRIM_WITH_INDEX = 101


class EmptyParseOp : ParseOp {
    override val name: String = "Empty"

    override val type: Int = TYPE_EMPTY

    override val description: String
        get() = STRINGS.parser.emptyParser

    override fun process(
        parts: Sequence<String>,
        parseMetadata: LogParser.ParseMetadata
    ): Sequence<String> {
        return parts
    }
}

/**
 * Treat the entire line as a single part, which means displaying the entire line in one column.
 */
class EmptySplitToPartsOp : SplitToPartsOp {
    override val name: String = "EmptySplitToParts"

    override val type: Int = OP_EMPTY_SPLIT_TO_PARTS

    override val description: String
        get() = STRINGS.parser.emptySplitToParts

    override fun process(
        parts: Sequence<String>,
        parseMetadata: LogParser.ParseMetadata
    ): Sequence<String> {
        return parts
    }

    override val splitPostProcessOps: List<SplitPostProcessOp> = emptyList()

    override fun equals(other: Any?): Boolean {
        return other is EmptySplitToPartsOp
    }

    override fun hashCode(): Int {
        return javaClass.name.hashCode()
    }
}

class SplitByWordSeparatorOp(
    val maxParts: Int = DEFAULT_MAX_PARTS,
    override val splitPostProcessOps: List<SplitPostProcessOp> = emptyList()
) : SplitToPartsOp {
    override val name: String = "SplitByWordSeparator"

    override val type: Int = OP_SPLIT_BY_WORD_SEPARATOR

    override val description: String
        get() = STRINGS.parser.splitByWordSeparator

    override fun process(
        parts: Sequence<String>,
        parseMetadata: LogParser.ParseMetadata
    ): Sequence<String> {
        var result: Sequence<String> = parts.flatMap { it.splitToSequence(WORD_REGEX, maxParts) }
        splitPostProcessOps.forEach {
            result = it.process(result, parseMetadata)
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is SplitByWordSeparatorOp
                && other.maxParts == maxParts
                && other.splitPostProcessOps == splitPostProcessOps
    }

    override fun hashCode(): Int {
        return Objects.hash(javaClass.name, maxParts, splitPostProcessOps)
    }

    companion object {
        const val DEFAULT_MAX_PARTS = 20
    }
}

class ExistedParserOp(
    val clazz: String = EMPTY_STRING,
    val jarPath: String = EMPTY_STRING,
    val isBuiltIn: Boolean = true
) : SplitToPartsOp {
    override val splitPostProcessOps: List<SplitPostProcessOp>
        get() = emptyList()
    override val name: String
        get() = "ExistedParser"
    override val description: String
        get() = EMPTY_STRING
    override val type: Int
        get() = OP_EXISTED_PARSER
    private val parserManager by lazy {
        ServiceManager.getContextService(ParserManager::class.java)
    }

    override fun process(
        parts: Sequence<String>,
        parseMetadata: LogParser.ParseMetadata
    ): Sequence<String> {
        val parser = parserManager.getParser(clazz, jarPath, isBuiltIn)
        return runCatching {
            if (parser == null) {
                Log.e(name, "[process] Parser not found: $clazz")
                return parts
            }
            val line = parts.firstOrNull() ?: return@runCatching parts
            val result = parser.parse(line, parseMetadata)
            result.asSequence()
        }.onFailure {
            Log.e(name, "[process] Error parsing with existed parser.", it)
        }.getOrDefault(parts)
    }

    override fun equals(other: Any?): Boolean {
        return other is ExistedParserOp
                && other.clazz == clazz
                && other.jarPath == jarPath
                && other.isBuiltIn == isBuiltIn
    }

    override fun hashCode(): Int {
        return Objects.hash(javaClass.name, clazz, jarPath, isBuiltIn)
    }
}

class SplitPartWithCharOp(val splitChar: Char?, val partIndex: Int, val maxPart: Int) : SplitPostProcessOp {

    override val name: String = "SplitPartWithChar"

    override val type: Int = OP_SPLIT_PART_WITH_CHAR

    override val description: String
        get() = STRINGS.parser.splitPartWithChar

    override fun process(
        parts: Sequence<String>,
        parseMetadata: LogParser.ParseMetadata
    ): Sequence<String> {
        splitChar ?: return parts
        return sequence {
            parts.forEachIndexed { index, s ->
                if (index == partIndex) {
                    if (maxPart == 0) {
                        yieldAll(s.splitToSequence(splitChar))
                    } else {
                        yieldAll(s.splitToSequence(splitChar, ignoreCase = false, limit = maxPart))
                    }
                } else {
                    yield(s)
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is SplitPartWithCharOp
                && other.splitChar == splitChar
                && other.partIndex == partIndex
                && other.maxPart == maxPart
    }

    override fun hashCode(): Int {
        return Objects.hash(javaClass.name, splitChar, partIndex, maxPart)
    }

}

class MergeNearbyPartsOp(val from: Int, val to: Int) : SplitPostProcessOp {

    override val name: String = "MergeNearbyParts"

    override val type: Int = OP_MERGE_NEARBY_PARTS

    override val description: String
        get() = STRINGS.parser.mergeNearbyParts

    override fun process(
        parts: Sequence<String>,
        parseMetadata: LogParser.ParseMetadata
    ): Sequence<String> = sequence {
        val mergedPart = StringBuilder()
        var index = 0

        parts.forEach { part ->
            when {
                index < from -> yield(part)
                index in from until to -> mergedPart.append(part).append(SPACE_STRING)
                index == to -> {
                    mergedPart.append(part)
                    yield(mergedPart.toString())
                }

                else -> yield(part)
            }
            index++
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is MergeNearbyPartsOp
                && other.from == from
                && other.to == to
    }

    override fun hashCode(): Int {
        return Objects.hash(javaClass.name, from, to)
    }

}

class MergeUntilCharOp(val start: Int, val targetChar: Char?) : SplitPostProcessOp {

    override val name: String = "MergeUntilChar"

    override val type: Int = OP_MERGE_UNTIL_CHAR

    override val description: String
        get() = STRINGS.parser.mergeUntilChar

    override fun process(
        parts: Sequence<String>,
        parseMetadata: LogParser.ParseMetadata
    ): Sequence<String> {
        return sequence {
            val mergedPart = StringBuilder()
            if (targetChar == null) {
                parts.forEachIndexed { index, s ->
                    if (index < start) {
                        yield(s)
                    } else {
                        mergedPart.append(s)
                    }
                }
                if (mergedPart.isNotEmpty()) {
                    yield(mergedPart.toString())
                }
                return@sequence
            }

            var index = 0
            var mergedComplete = false
            parts.forEach { part ->
                if (index < start) {
                    yield(part)
                } else {
                    if (!mergedComplete && part.contains(targetChar)) {
                        val strBeforeMergeChar = part.substringBefore(targetChar)
                        val strAfterMergeChar = part.substringAfter(targetChar)
                        mergedPart.append(strBeforeMergeChar)
                        yield(mergedPart.toString())
                        if (strAfterMergeChar.isNotBlank()) {
                            yield(strAfterMergeChar)
                        }
                        mergedComplete = true
                    } else if (!mergedComplete) {
                        mergedPart.append(part)
                    } else {
                        yield(part)
                    }
                }
                index++
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is MergeUntilCharOp
                && other.start == start
                && other.targetChar == targetChar
    }

    override fun hashCode(): Int {
        return Objects.hash(javaClass.name, start, targetChar)
    }

}

class RemoveBlankPartOp : SplitPostProcessOp {
    override val name: String = "RemoveBlankPart"

    override val type: Int = OP_REMOVE_BLANK_PART

    override val description: String
        get() = STRINGS.parser.removeBlankPart

    override fun process(
        parts: Sequence<String>,
        parseMetadata: LogParser.ParseMetadata
    ): Sequence<String> {
        return parts.filter { it.isNotBlank() }
    }

    override fun equals(other: Any?): Boolean {
        return other is RemoveBlankPartOp
    }

    override fun hashCode(): Int {
        return javaClass.name.hashCode()
    }
}

interface TrimOp : ParseOp {
    val partIndex: Int
}

/**
 * Delete specific characters from the beginning and end
 */
class TrimWithCharOp(
    override val partIndex: Int,
    val leading: Char?,
    val trailing: Char?
) : TrimOp {
    override val name: String = "TrimWithChar"

    override val type: Int = OP_TRIM_WITH_CHAR

    override val description: String
        get() = STRINGS.parser.trimWithChar

    override fun process(
        parts: Sequence<String>,
        parseMetadata: LogParser.ParseMetadata
    ): Sequence<String> {
        if (leading == null && trailing == null) return parts
        return parts.mapIndexed { index, s ->
            var result = s
            if (index == partIndex) {
                if (leading != null) {
                    result = result.trimStart(leading)
                }
                if (trailing != null) {
                    result = result.trimEnd(trailing)
                }
            }
            result
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is TrimWithCharOp
                && other.partIndex == partIndex
                && other.leading == leading
                && other.trailing == trailing
    }

    override fun hashCode(): Int {
        return Objects.hash(javaClass.name, partIndex, leading, trailing)
    }

}

/**
 * [removedLeadingCharCount] The length to be removed from the beginning,
 * [removedTrailingCharCount] The length to be removed from the end
 */
class TrimWithIndexOp(
    override val partIndex: Int,
    val removedLeadingCharCount: Int,
    val removedTrailingCharCount: Int
) : TrimOp {
    override val name: String = "TrimWithIndex"

    override val type: Int = OP_TRIM_WITH_INDEX

    override val description: String
        get() = STRINGS.parser.trimWithIndex

    override fun process(
        parts: Sequence<String>,
        parseMetadata: LogParser.ParseMetadata
    ): Sequence<String> {
        return parts.mapIndexed { index, s ->
            if (index == partIndex) {
                if (removedTrailingCharCount + removedLeadingCharCount > s.length) {
                    EMPTY_STRING
                } else {
                    s.substring(removedLeadingCharCount, s.length - removedTrailingCharCount)
                }
            } else {
                s
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is TrimWithIndexOp
                && other.partIndex == partIndex
                && other.removedLeadingCharCount == removedLeadingCharCount
                && other.removedTrailingCharCount == removedTrailingCharCount
    }

    override fun hashCode(): Int {
        return Objects.hash(javaClass.name, partIndex, removedLeadingCharCount, removedTrailingCharCount)
    }

}

