package me.gegenbauer.catspy.log.metadata

import me.gegenbauer.catspy.configuration.GlobalStrings
import me.gegenbauer.catspy.log.parse.LogParser
import me.gegenbauer.catspy.log.serialize.LogMetadataModel
import me.gegenbauer.catspy.utils.IdGenerator
import me.gegenbauer.catspy.view.color.DarkThemeAwareColor
import java.awt.Color

/**
 * Do not use constructor to create an instance of [LogMetadata]. Use [LogMetadataModel] instead.
 * [LogMetadataModel] is used to edit and save log metadata.
 */
class LogMetadata(
    val logType: String,
    val parser: LogParser,
    val columns: List<Column>,
    val supportedFileExtensions: Set<String>,
    val isDeviceLog: Boolean = false,
    val isBuiltIn: Boolean = true,
    val description: String = "",
    val sample: String = "",
    val levels: List<DisplayedLevel> = emptyList()
) {

    companion object {
        const val KEY = "logMetaData"

        val default = LogMetadata("", LogParser.empty, emptyList(), emptySet())
    }
}

interface Column {
    val id: Int

    val name: String

    val supportFilter: Boolean

    /**
     * Whether the content of this column is parsed from the original log or added after parsing.
     * For example, the package column of the device log is added after parsing.
     */
    val isParsed: Boolean

    val uiConf: UIConf

    /**
     * The index of a specific item after the log is divided, and the filter corresponding to the column is
     * also sorted according to it. After the log line is parsed by the log parser, custom content may be added,
     * such as the package name information for device logs.
     */
    val partIndex: Int

    data class UIConf(
        val column: ColumnUIConf,
        val filter: CommonFilterUIConf = CommonFilterUIConf()
    )

    data class ColumnUIConf(
        val index: Int = -1,
        val charLen: Int = DEFAULT_COLUMN_CHAR_LEN,
        val isHidden: Boolean = false,
    )

    open class DefaultColumn(
        override val name: String,
        override val supportFilter: Boolean = true,
        override val isParsed: Boolean = true,
        override val uiConf: UIConf,
        override val partIndex: Int = -1
    ) : Column {
        override val id: Int = IdGenerator.generateId()
    }

    class LevelColumn(
        name: String,
        supportFilter: Boolean,
        uiConf: UIConf,
        partIndex: Int,
        val levels: List<DisplayedLevel>
    ) : DefaultColumn(name, supportFilter, true, uiConf, partIndex)

    class MessageColumn(
        name: String,
        supportFilter: Boolean,
        uiConf: UIConf,
        partIndex: Int,
    ) : DefaultColumn(name, supportFilter, true, uiConf, partIndex)

    open class CommonFilterUIConf(
        val name: String = "",
        val layoutWidth: Double = 0.0,
        val position: FilterPosition = FilterPosition(0, 0),
        val columnId: Int = -1,
        val columnName: String = ""
    )

    class LevelFilterUIConf(
        name: String,
        val levels: List<DisplayedLevel>,
        layoutWidth: Double = LAYOUT_WIDTH_PREFERRED,
        position: FilterPosition = FilterPosition(0, 0),
        columnId: Int = -1,
        columnName: String = ""
    ) : CommonFilterUIConf(name, layoutWidth, position, columnId, columnName)

    class MatchCaseFilterInfoConf :
        CommonFilterUIConf(GlobalStrings.MATCH_CASE, LAYOUT_WIDTH_PREFERRED, FilterPosition(Int.MAX_VALUE))

    data class FilterPosition(
        val columnIndex: Int,
        val rowIndex: Int = 0,
    )

    companion object {
        const val LAYOUT_WIDTH_FILL = -1.0
        const val LAYOUT_WIDTH_PREFERRED = -2.0
        private const val DEFAULT_COLUMN_CHAR_LEN = 10

        val default = DefaultColumn(
            "",
            supportFilter = true,
            isParsed = true,
            uiConf = UIConf(ColumnUIConf(), CommonFilterUIConf()),
            partIndex = 0
        )
    }
}

fun LogMetadata.getFilterUIConfs(): List<Column.CommonFilterUIConf> {
    val filterUIConfs = columns.sortedBy { it.partIndex }
        .filter { it.supportFilter && it.uiConf.column.isHidden.not() }
        .map { it.uiConf.filter }
        .toMutableList()
    filterUIConfs.add(Column.MatchCaseFilterInfoConf())
    return filterUIConfs
}

data class Level(
    val value: Int,
    val name: String,
    val tag: String,
) {
    companion object {
        val default: Level = Level(0, "Default", "D")
    }
}

fun Level.toDisplayedLevel(color: DarkThemeAwareColor): DisplayedLevel = DisplayedLevel(this, color)

data class DisplayedLevel(
    val level: Level,
    val color: DarkThemeAwareColor
) {

    constructor(value: Int, name: String, tag: String, color: DarkThemeAwareColor) : this(
        Level(value, name, tag),
        color
    )

    companion object {
        val default: DisplayedLevel = DisplayedLevel(Level.default, DarkThemeAwareColor(Color.BLACK))
    }
}

interface LogMetadataOwner {
    var logMetaData: LogMetadata
}