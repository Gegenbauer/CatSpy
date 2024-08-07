package me.gegenbauer.catspy.log.serialize

import com.google.gson.*
import me.gegenbauer.catspy.log.metadata.DisplayedLevel
import me.gegenbauer.catspy.log.parse.LogParser
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.file.Serializer
import java.lang.reflect.Type

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
            val opType = jsonObject[KEY_SPLIT_TO_PARTS_OP_TYPE].asInt
            val opClass = splitToPartsOps[opType] ?: throw IllegalArgumentException("Unknown op type: $opType")
            return gson.fromJson(json, opClass)
        }

        override fun serialize(src: SplitToPartsOp, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            val jsonObject = gson.toJsonTree(src).asJsonObject
            jsonObject.addProperty(
                KEY_SPLIT_TO_PARTS_OP_TYPE, when (src) {
                    is SplitByWordSeparatorOp -> OP_SPLIT_BY_WORD_SEPARATOR
                    is EmptySplitToPartsOp -> OP_EMPTY_SPLIT_TO_PARTS
                    else -> throw IllegalArgumentException("Unknown op type: $src")
                }
            )
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
            val opType = jsonObject[KEY_SPLIT_POST_PROCESS_OP_TYPE].asInt
            val opClass = preProcessPostOps[opType] ?: throw IllegalArgumentException("Unknown op type: $opType")
            return gson.fromJson(json, opClass)
        }

        override fun serialize(
            src: SplitPostProcessOp,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val jsonObject = gson.toJsonTree(src).asJsonObject
            jsonObject.addProperty(
                KEY_SPLIT_POST_PROCESS_OP_TYPE, when (src) {
                    is SplitPartWithCharOp -> OP_SPLIT_PART_WITH_CHAR
                    is MergeNearbyPartsOp -> OP_MERGE_NEARBY_PARTS
                    is RemoveBlankPartOp -> OP_REMOVE_BLANK_PART
                    is MergeUntilCharOp -> OP_MERGE_UNTIL_CHAR
                    else -> throw IllegalArgumentException("Unknown op type: $src")
                }
            )
            return jsonObject
        }
    }

    class TrimOpAdapter : JsonDeserializer<TrimOp>, JsonSerializer<TrimOp> {
        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): TrimOp {
            val jsonObject = json.asJsonObject
            val opType = jsonObject[KEY_TRIM_OP_TYPE].asInt
            val opClass = parseOps[opType] ?: throw IllegalArgumentException("Unknown op type: $opType")
            return gson.fromJson(json, opClass)
        }

        override fun serialize(src: TrimOp, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            val jsonObject = gson.toJsonTree(src).asJsonObject
            jsonObject.addProperty(
                KEY_TRIM_OP_TYPE, when (src) {
                    is TrimWithCharOp -> OP_TRIM_WITH_CHAR
                    is TrimWithIndexOp -> OP_TRIM_WITH_INDEX
                    else -> throw IllegalArgumentException("Unknown op type: $src")
                }
            )
            return jsonObject
        }
    }

    companion object {
        private const val OP_EMPTY_SPLIT_TO_PARTS = 0
        private const val OP_SPLIT_BY_WORD_SEPARATOR = 1

        private const val OP_SPLIT_PART_WITH_CHAR = 10
        private const val OP_MERGE_NEARBY_PARTS = 11
        private const val OP_REMOVE_BLANK_PART = 12
        private const val OP_MERGE_UNTIL_CHAR = 13

        private const val OP_TRIM_WITH_CHAR = 100
        private const val OP_TRIM_WITH_INDEX = 101

        private const val KEY_SPLIT_TO_PARTS_OP_TYPE = "splitToPartsOpType"
        private const val KEY_SPLIT_POST_PROCESS_OP_TYPE = "splitPostProcessOpType"
        private const val KEY_TRIM_OP_TYPE = "trimOpType"

        private val splitToPartsOps = mutableMapOf<Int, Class<out SplitToPartsOp>>()
        private val preProcessPostOps = mutableMapOf<Int, Class<out SplitPostProcessOp>>()
        private val parseOps = mutableMapOf<Int, Class<out TrimOp>>()

        private val gson = GsonBuilder()
            .registerTypeAdapter(SplitToPartsOp::class.java, PreProcessOpAdapter())
            .registerTypeAdapter(SplitPostProcessOp::class.java, PreProcessPostOpAdapter())
            .registerTypeAdapter(TrimOp::class.java, TrimOpAdapter())
            .create()

        init {
            splitToPartsOps[OP_SPLIT_BY_WORD_SEPARATOR] = SplitByWordSeparatorOp::class.java
            splitToPartsOps[OP_EMPTY_SPLIT_TO_PARTS] = EmptySplitToPartsOp::class.java
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

    private var partCount: Int = 0
    private var levelPartIndex: Int = 0
    private var defaultLevelTag: String = DisplayedLevel.default.level.tag

    fun setLogMetadata(logMetadataModel: LogMetadataModel) {
        val parsedColumns = logMetadataModel.columns.sortedBy { it.partIndex }.filter { it.isParsed }
        val columnCount = parsedColumns.size
        val levelPartIndex = parsedColumns.indexOfFirst { it.isLevel }
        val defaultLevelTag = logMetadataModel.levels.minByOrNull { it.level.value }?.level?.tag ?: ""
        configure(columnCount, levelPartIndex, defaultLevelTag)
    }

    private fun configure(partCount: Int, levelPartIndex: Int, defaultLevelTag: String) {
        this.partCount = partCount
        this.levelPartIndex = levelPartIndex
        this.defaultLevelTag = defaultLevelTag
    }

    override fun parse(line: String): List<String> {
        val splitResult = splitToPartsOp.process(sequenceOf(line))
        val defaultResult = run {
            val default = Array(partCount) { "" }
            if (levelPartIndex > 0) default[levelPartIndex] = defaultLevelTag
            default[default.size - 1] = line
            default.toList()
        }
        val result = kotlin.runCatching {
            if (trimOps.isEmpty()) {
                splitResult
            } else {
                trimOps.fold(splitResult) { parts, op ->
                    op.process(parts)
                }
            }
        }.getOrDefault(defaultResult.asSequence()).toList()
        return result.takeIf { it.size >= partCount } ?: defaultResult
    }

    override fun equals(other: Any?): Boolean {
        return other is SerializableLogParser
                && other.splitToPartsOp == splitToPartsOp
                && other.trimOps == trimOps
    }

    override fun hashCode(): Int {
        return javaClass.name.hashCode() * 31 + splitToPartsOp.hashCode() * 31 + trimOps.hashCode()
    }
}

interface ParseOp {
    val name: String

    val description: String

    fun process(parts: Sequence<String>): Sequence<String>
}

/**
 * Preprocessing operation, which refers to splitting the original log line into multiple parts through extraction or splitting.
 * The final result is a sequence of strings, each representing a part, which can also be considered as a column in a table.
 */
interface SplitToPartsOp : ParseOp {
    val splitPostProcessOps: List<SplitPostProcessOp>
}

/**
 * 后处理操作，指对分割后的部分进行处理，对每个部分进一步分割，或者合并相邻部分，或者移除空白部分等
 */
interface SplitPostProcessOp : ParseOp

class EmptyParseOp : ParseOp {
    override val name: String = "Empty"

    override val description: String
        get() = STRINGS.parser.emptyParser

    override fun process(parts: Sequence<String>): Sequence<String> {
        return parts
    }
}

/**
 * Treat the entire line as a single part, which means displaying the entire line in one column.
 */
class EmptySplitToPartsOp : SplitToPartsOp {
    override val name: String = "EmptySplitToParts"

    override val description: String
        get() = STRINGS.parser.emptySplitToParts

    override fun process(parts: Sequence<String>): Sequence<String> {
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

    override val description: String
        get() = STRINGS.parser.splitByWordSeparator

    override fun process(parts: Sequence<String>): Sequence<String> {
        var result: Sequence<String> = parts.flatMap { it.splitToSequence(regex, maxParts) }
        splitPostProcessOps.forEach {
            result = it.process(result)
        }
        return result
    }

    override fun equals(other: Any?): Boolean {
        return other is SplitByWordSeparatorOp
                && other.maxParts == maxParts
                && other.splitPostProcessOps == splitPostProcessOps
    }

    override fun hashCode(): Int {
        return javaClass.name.hashCode() * 31 + maxParts.hashCode() * 31 + splitPostProcessOps.hashCode()
    }

    companion object {
        const val DEFAULT_MAX_PARTS = 20
        private val regex = "\\s+".toRegex()
    }
}

class SplitPartWithCharOp(val splitChar: Char?, val partIndex: Int) : SplitPostProcessOp {

    override val name: String = "SplitPartWithChar"

    override val description: String
        get() = STRINGS.parser.splitPartWithChar

    override fun process(parts: Sequence<String>): Sequence<String> {
        splitChar ?: return parts
        return sequence {
            parts.forEachIndexed { index, s ->
                if (index == partIndex) {
                    yieldAll(s.splitToSequence(splitChar))
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
    }

    override fun hashCode(): Int {
        return javaClass.name.hashCode() * 31 + splitChar.hashCode() * 31 + partIndex.hashCode()
    }

}

class MergeNearbyPartsOp(val from: Int, val to: Int) : SplitPostProcessOp {

    override val name: String = "MergeNearbyParts"

    override val description: String
        get() = STRINGS.parser.mergeNearbyParts

    override fun process(parts: Sequence<String>): Sequence<String> = sequence {
        val mergedPart = StringBuilder()
        var index = 0

        parts.forEach { part ->
            when {
                index < from -> yield(part)
                index in from until to -> mergedPart.append(part).append(" ")
                index == to -> {
                    mergedPart.append(part)
                    yield(mergedPart.toString().trim())
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
        return javaClass.name.hashCode() * 31 + from.hashCode() * 31 + to.hashCode()
    }

}

class MergeUntilCharOp(val start: Int, val targetChar: Char?) : SplitPostProcessOp {

    override val name: String = "MergeUntilChar"

    override val description: String
        get() = STRINGS.parser.mergeUntilChar

    override fun process(parts: Sequence<String>): Sequence<String> {
        targetChar ?: return parts
        return sequence {
            val mergedPart = StringBuilder()
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
        return javaClass.name.hashCode() * 31 + start.hashCode() * 31 + targetChar.hashCode()
    }

}

class RemoveBlankPartOp : SplitPostProcessOp {
    override val name: String = "RemoveBlankPart"

    override val description: String
        get() = STRINGS.parser.removeBlankPart

    override fun process(parts: Sequence<String>): Sequence<String> {
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

    override val description: String
        get() = STRINGS.parser.trimWithChar

    override fun process(parts: Sequence<String>): Sequence<String> {
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
        return javaClass.name.hashCode() * 31 + partIndex.hashCode() * 31 + leading.hashCode() * 31 + trailing.hashCode()
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
) :
    TrimOp {

    override val name: String = "TrimWithIndex"

    override val description: String
        get() = STRINGS.parser.trimWithIndex

    override fun process(parts: Sequence<String>): Sequence<String> {
        return parts.mapIndexed { index, s ->
            if (index == partIndex) {
                if (removedTrailingCharCount + removedLeadingCharCount > s.length) {
                    ""
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
        return javaClass.name.hashCode() * 31 + partIndex.hashCode() * 31 + removedLeadingCharCount.hashCode() * 31 + removedTrailingCharCount.hashCode()
    }

}

