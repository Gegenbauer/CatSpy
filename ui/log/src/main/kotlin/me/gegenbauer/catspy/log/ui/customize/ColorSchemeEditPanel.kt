package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.metadata.LogColorScheme
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import me.gegenbauer.catspy.view.color.DarkThemeAwareColor
import java.awt.Color
import java.lang.reflect.Modifier
import javax.swing.table.DefaultTableModel

class ColorSchemeEditPanel : BaseEditableTablePanel<ColorSchemeItem>() {

    override val tableName: String = STRINGS.ui.tableColorSchemeInfo
    override val columnParams: List<ColumnParam>
         = listOf(
            ColumnParam(
                STRINGS.ui.colorName,
                java.lang.String::class.java,
                tooltip = STRINGS.toolTip.colorName,
                neverEditable = true
            ),
            ColumnParam(
                STRINGS.ui.colorEditColorColumn,
                Color::class.java,
                editableWhenBuiltIn = true,
                tooltip = run {
                    val theme = if (isNightMode) STRINGS.ui.darkTheme else STRINGS.ui.lightTheme
                    STRINGS.toolTip.colorEditColorColumn.get(theme)
                },
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
                ColorSchemeItem(name, color)
            }
    }

    override fun createNewItem(): ColorSchemeItem {
        // will not be used
        return ColorSchemeItem(EMPTY_STRING, DarkThemeAwareColor(Color.WHITE, Color.BLACK))
    }

    override fun ColorSchemeItem.toRowItem(): List<Any?> {
        return listOf(name, getCurrentColor(color))
    }

    override fun getUpdatedItems(): List<ColorSchemeItem> {
        val tableModel = table.model as DefaultTableModel
        val items = mutableListOf<ColorSchemeItem>()
        for (i in 0 until tableModel.rowCount) {
            val name = tableModel.getValueAt(i, 0) as String
            val newColor = tableModel.getValueAt(i, 1) as Color
            val currentColor =
                originalItems.find { it.name == name }?.color ?: DarkThemeAwareColor(Color.WHITE, Color.BLACK)
            val targetColor = getDarkThemeAwareColor(currentColor, newColor)
            items.add(ColorSchemeItem(name, targetColor))
        }
        return items
    }

}

data class ColorSchemeItem(
    val name: String,
    val color: DarkThemeAwareColor
)