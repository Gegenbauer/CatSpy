package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.log.metadata.DisplayedLevel
import me.gegenbauer.catspy.log.metadata.Level
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.color.DarkThemeAwareColor
import java.awt.Color
import java.awt.Component
import javax.swing.AbstractCellEditor
import javax.swing.JButton
import javax.swing.JColorChooser
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor

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
            STRINGS.ui.levelLightColor,
            Color::class.java,
            editableWhenBuiltIn = true,
            tooltip = STRINGS.toolTip.levelLightColor
        ),
        ColumnParam(
            STRINGS.ui.levelDarkColor,
            Color::class.java,
            editableWhenBuiltIn = true,
            tooltip = STRINGS.toolTip.levelDarkColor
        )
    )

    override val hint: String
        get() = STRINGS.ui.tableLevelInfoHint

    override fun setLogMetadata(metadata: LogMetadataEditModel) {
        super.setLogMetadata(metadata)
        items = metadata.model.levels
    }

    override fun configure() {
        super.configure()
        configureTableColorColumns(table)
    }

    private fun configureTableColorColumns(table: JTable) {
        COLOR_COLUMN_INDEXES.forEach {
            table.columnModel.getColumn(it).cellRenderer = ColorRenderer()
            table.columnModel.getColumn(it).cellEditor = ColorEditor()
        }
    }

    override fun getUpdatedItems(): List<DisplayedLevel> {
        val tableModel = table.model as DefaultTableModel
        val levels = mutableListOf<DisplayedLevel>()

        for (i in 0 until tableModel.rowCount) {
            val levelName = tableModel.getValueAt(i, 0) as String
            val levelTag = tableModel.getValueAt(i, 1) as String
            val lightColor = tableModel.getValueAt(i, 2) as Color
            val darkColor = tableModel.getValueAt(i, 3) as Color
            levels.add(
                DisplayedLevel(
                    Level(i, levelName, levelTag),
                    DarkThemeAwareColor(lightColor, darkColor)
                )
            )
        }

        return levels
    }

    override fun createNewItem(): DisplayedLevel {
        return DisplayedLevel(Level(0, "New Level", "NL"), DarkThemeAwareColor(Color.WHITE, Color.BLACK))
    }

    override fun DisplayedLevel.toRowItem(): List<Any> {
        return listOf(
            level.name,
            level.tag,
            color.dayColor,
            color.nightColor
        )
    }

    companion object {
        private val COLOR_COLUMN_INDEXES = listOf(2, 3)
    }

}