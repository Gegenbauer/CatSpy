package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.metadata.DisplayedLevel
import me.gegenbauer.catspy.log.metadata.Level
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.color.DarkThemeAwareColor
import java.awt.Color
import javax.swing.table.DefaultTableModel

class LevelsEditPanel : BaseEditableTablePanel<DisplayedLevel>() {

    override val tableName: String = STRINGS.ui.tableLevelInfo

    override val columnParams: List<ColumnParam> = listOf(
        ColumnParam(
            STRINGS.ui.levelName,
            java.lang.String::class.java,
            tooltip = STRINGS.toolTip.levelName
        ),
        ColumnParam(
            STRINGS.ui.levelAbbreviation,
            java.lang.String::class.java,
            tooltip = STRINGS.toolTip.levelAbbreviation
        ),
        ColumnParam(
            STRINGS.ui.levelKeyword,
            java.lang.String::class.java,
            tooltip = STRINGS.toolTip.levelKeyword
        ),
        ColumnParam(
            STRINGS.ui.logForeground,
            Color::class.java,
            editableWhenBuiltIn = true,
            tooltip = STRINGS.toolTip.logForeground
        ),
        ColumnParam(
            STRINGS.ui.levelColumnForeground,
            Color::class.java,
            editableWhenBuiltIn = true,
            tooltip = STRINGS.toolTip.levelColumnForeground
        ),
        ColumnParam(
            STRINGS.ui.levelColumnBackground,
            Color::class.java,
            editableWhenBuiltIn = true,
            tooltip = STRINGS.toolTip.levelColumnBackground
        )
    )

    override val hint: String
        get() = STRINGS.ui.tableLevelInfoHint

    override fun setLogMetadata(metadata: LogMetadataEditModel) {
        super.setLogMetadata(metadata)
        items = metadata.model.levels
    }

    override fun getUpdatedItems(): List<DisplayedLevel> {
        val tableModel = table.model as DefaultTableModel
        val levels = mutableListOf<DisplayedLevel>()

        for (i in 0 until tableModel.rowCount) {
            val levelName = tableModel.getValueAt(i, 0) as String
            val levelAbbreviation = tableModel.getValueAt(i, 1) as String
            val levelKeyword = tableModel.getValueAt(i, 2) as String
            val logForeground = tableModel.getValueAt(i, 3) as Color
            val currentLogForeground = originalItems.getOrNull(i)?.logForeground ?: DarkThemeAwareColor(Color.BLACK)
            val targetLogForeground = getDarkThemeAwareColor(currentLogForeground, logForeground)

            val levelColumnForeground = tableModel.getValueAt(i, 4) as Color
            val currentLevelColumnForeground =
                originalItems.getOrNull(i)?.levelColumnForeground ?: DarkThemeAwareColor(Color.BLACK)
            val targetLevelColumnForeground =
                getDarkThemeAwareColor(currentLevelColumnForeground, levelColumnForeground)

            val levelColumnBackground = tableModel.getValueAt(i, 5) as Color
            val currentLevelColumnBackground =
                originalItems.getOrNull(i)?.levelColumnBackground ?: DarkThemeAwareColor(Color.BLACK)
            val targetLevelColumnBackground =
                getDarkThemeAwareColor(currentLevelColumnBackground, levelColumnBackground)
            levels.add(
                DisplayedLevel(
                    Level(i, levelName, levelAbbreviation, levelKeyword),
                    targetLogForeground,
                    targetLevelColumnForeground,
                    targetLevelColumnBackground
                )
            )
        }

        return levels
    }

    override fun createNewItem(): DisplayedLevel {
        return DisplayedLevel(
            Level(0, "New Level", "NL", "NL"),
            DarkThemeAwareColor(Color.WHITE, Color.BLACK),
            DarkThemeAwareColor(Color.WHITE, Color.BLACK),
            DarkThemeAwareColor(Color.WHITE, Color.BLACK),
        )
    }

    override fun DisplayedLevel.toRowItem(): List<Any> {
        return listOf(
            level.name,
            level.abbreviation,
            level.keyword,
            getCurrentColor(logForeground),
            getCurrentColor(levelColumnForeground),
            getCurrentColor(levelColumnBackground)
        )
    }

}