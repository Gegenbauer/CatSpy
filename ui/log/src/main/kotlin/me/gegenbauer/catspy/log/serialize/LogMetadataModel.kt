package me.gegenbauer.catspy.log.serialize

import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.metadata.Column
import me.gegenbauer.catspy.log.metadata.Column.Companion.LAYOUT_WIDTH_PREFERRED
import me.gegenbauer.catspy.log.metadata.Column.FilterPosition
import me.gegenbauer.catspy.log.metadata.DisplayedLevel
import me.gegenbauer.catspy.log.metadata.LogColorScheme
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.parse.LogParser

/**
 * Before [LogMetadata] is serialized, it needs to be converted to a [LogMetadataModel] instance,
 * and then serialized. The locally stored instance is [LogMetadataModel].
 * To obtain a [LogMetadata] instance, the locally stored [LogMetadataModel] JSON copy needs to be parsed into
 * a [LogMetadataModel] instance, and then converted to a [LogMetadata] instance.
 */
data class LogMetadataModel(
    val logType: String,
    val parser: LogParser,
    val columns: List<ColumnModel>,
    val levels: List<DisplayedLevel>,
    val colorScheme: LogColorScheme,
    val isDeviceLog: Boolean,
    val isBuiltIn: Boolean,
    val description: String,
    val sample: String,
    // used to check if the metadata is outdated and update or discard it if necessary
    val version: Int
) {
    companion object {
        val default = LogMetadataModel(
            EMPTY_STRING,
            LogParser.empty,
            emptyList(),
            emptyList(),
            LogColorScheme(),
            isDeviceLog = false,
            isBuiltIn = false,
            description = EMPTY_STRING,
            sample = EMPTY_STRING,
            version = LogMetadata.VERSION
        )
    }
}

data class ColumnModel(
    val name: String,
    val supportFilter: Boolean,
    val isParsed: Boolean = false,
    val uiConf: UIConf,
    val partIndex: Int,
    val isMessage: Boolean,
    val isLevel: Boolean,
    @Transient val id: Int
) {
    data class UIConf(
        val column: ColumnUIConf,
        val filter: FilterUIConf
    )

    data class ColumnUIConf(
        val index: Int,
        val charLen: Int,
        val isHidden: Boolean
    )

    data class FilterUIConf(
        val name: String,
        val layoutWidth: Double,
        val position: FilterPosition,
        @Transient val columnId: Int,
        @Transient val columnName: String,
    ) {
        companion object {
            val default = FilterUIConf(
                EMPTY_STRING, LAYOUT_WIDTH_PREFERRED, FilterPosition(
                    0, 0
                ), -1, EMPTY_STRING
            )
        }
    }

    companion object {
        val default = ColumnModel(
            EMPTY_STRING,
            supportFilter = true,
            isParsed = true,
            UIConf(
                ColumnUIConf(0, -1, false),
                FilterUIConf.default
            ),
            -1,
            isMessage = false,
            isLevel = false,
            id = -1
        )
    }
}

fun LogMetadata.toLogMetadataModel(): LogMetadataModel {
    return LogMetadataModel(
        logType,
        parser,
        columns.map { it.toColumnModel() },
        levels,
        colorScheme,
        isDeviceLog,
        isBuiltIn,
        description,
        sample,
        version
    )
}

private fun Column.toColumnModel(): ColumnModel {
    return ColumnModel(
        name,
        supportFilter,
        isParsed,
        uiConf.toUIConf(id, name),
        partIndex,
        this is Column.MessageColumn,
        this is Column.LevelColumn,
        id,
    )
}

private fun Column.UIConf.toUIConf(columnId: Int, columnName: String): ColumnModel.UIConf {
    return ColumnModel.UIConf(
        column.toColumnUIConf(),
        filter.toFilterUIConf(columnId, columnName)
    )
}

private fun Column.ColumnUIConf.toColumnUIConf(): ColumnModel.ColumnUIConf {
    return ColumnModel.ColumnUIConf(
        index,
        charLen,
        isHidden
    )
}

private fun Column.CommonFilterUIConf.toFilterUIConf(columnId: Int, columnName: String): ColumnModel.FilterUIConf {
    return ColumnModel.FilterUIConf(
        name,
        layoutWidth,
        position,
        columnId,
        columnName
    )
}

fun LogMetadataModel.toLogMetadata(): LogMetadata {
    val columns = columns.map { columnModel ->
        columnModel.toColumn(columnModel.isLevel, levels, columnModel.isMessage)
    }
    return LogMetadata(
        logType,
        parser,
        columns,
        isDeviceLog,
        isBuiltIn,
        description,
        sample,
        levels,
        colorScheme,
    )
}

private fun ColumnModel.toColumn(isLevel: Boolean, levels: List<DisplayedLevel>, isMessage: Boolean): Column {
    val columnUIConf = Column.ColumnUIConf(
        uiConf.column.index,
        uiConf.column.charLen,
        uiConf.column.isHidden
    )
    val filterUIConf = if (isLevel) {
        Column.LevelFilterUIConf(
            uiConf.filter.name,
            levels,
            uiConf.filter.layoutWidth,
            uiConf.filter.position,
            uiConf.filter.columnId
        )
    } else {
        Column.CommonFilterUIConf(
            uiConf.filter.name,
            uiConf.filter.layoutWidth,
            uiConf.filter.position,
            uiConf.filter.columnId,
            uiConf.filter.columnName
        )
    }
    return if (isLevel) {
        Column.LevelColumn(
            name,
            supportFilter,
            Column.UIConf(columnUIConf, filterUIConf),
            partIndex,
            levels,
        )
    } else if (isMessage) {
        Column.MessageColumn(
            name,
            supportFilter,
            Column.UIConf(columnUIConf, filterUIConf),
            partIndex
        )
    } else {
        Column.DefaultColumn(
            name,
            supportFilter,
            isParsed,
            Column.UIConf(columnUIConf, filterUIConf),
            partIndex
        )
    }
}
