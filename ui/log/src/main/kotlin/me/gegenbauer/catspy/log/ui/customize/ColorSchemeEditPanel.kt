package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.log.metadata.LogColorScheme
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.color.DarkThemeAwareColor
import java.awt.Color
import java.lang.reflect.Modifier
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class ColorSchemeEditPanel : BaseEditableTablePanel<ColorSchemeItem>() {

    override val tableName: String = STRINGS.ui.tableColorSchemeInfo
    override val columnParams: List<ColumnParam> =
        listOf(
            ColumnParam(
                STRINGS.ui.colorName,
                java.lang.String::class.java,
                tooltip = STRINGS.toolTip.colorName,
                neverEditable = true
            ),
            ColumnParam(
                STRINGS.ui.lightColor,
                Color::class.java,
                editableWhenBuiltIn = true,
                tooltip = STRINGS.toolTip.lightColor
            ),
            ColumnParam(
                STRINGS.ui.darkColor,
                Color::class.java,
                editableWhenBuiltIn = true,
                tooltip = STRINGS.toolTip.darkColor
            )
        )

    override val actionVisibilityParam: EditableTableActionPanel.ActionVisibilityParam =
        EditableTableActionPanel.ActionVisibilityParam(
            loadTemplateAction = EditableTableActionPanel.StateControlParam(alwaysTrue = true)
        )

    override fun setLogMetadata(metadata: LogMetadataEditModel) {
        super.setLogMetadata(metadata)
        items = createColorSchemeItems(metadata.model.colorScheme)
    }

    private fun createColorSchemeItems(logColorScheme: LogColorScheme): List<ColorSchemeItem> {
        return logColorScheme.javaClass.declaredFields
            .filter { it.type == DarkThemeAwareColor::class.java && !Modifier.isStatic(it.modifiers) }
            .map {
                it.isAccessible = true
                val name = it.name
                val color = it.get(logColorScheme) as DarkThemeAwareColor
                ColorSchemeItem(name, color.dayColor, color.nightColor)
            }
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

    override fun createNewItem(): ColorSchemeItem {
        // will not be used
        return ColorSchemeItem("", Color.BLACK, Color.BLACK)
    }

    override fun ColorSchemeItem.toRowItem(): List<Any?> {
        return listOf(name, lightColor, darkColor)
    }

    override fun getUpdatedItems(): List<ColorSchemeItem> {
        val tableModel = table.model as DefaultTableModel
        val items = mutableListOf<ColorSchemeItem>()
        for (i in 0 until tableModel.rowCount) {
            val name = tableModel.getValueAt(i, 0) as String
            val lightColor = tableModel.getValueAt(i, 1) as Color
            val darkColor = tableModel.getValueAt(i, 2) as Color
            items.add(ColorSchemeItem(name, lightColor, darkColor))
        }
        return items
    }

    companion object {
        private val COLOR_COLUMN_INDEXES = listOf(1, 2)
    }

}

data class ColorSchemeItem(
    val name: String,
    val lightColor: Color,
    val darkColor: Color
)