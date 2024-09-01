package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.java.ext.Bundle
import me.gegenbauer.catspy.log.metadata.DisplayedLevel
import me.gegenbauer.catspy.log.metadata.LogColorScheme
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.serialize.LogMetadataModel
import me.gegenbauer.catspy.log.serialize.toLogMetadata
import me.gegenbauer.catspy.log.ui.tab.FileLogMainPanel
import me.gegenbauer.catspy.view.color.DarkThemeAwareColor
import java.awt.Component
import java.lang.reflect.Modifier

/**
 * A panel that displays a preview of sample logs based on the given metadata.
 */
class LogMetadataPreviewPanel : FileLogMainPanel() {

    override val tag: String = "PreviewPanel"

    init {
        templateSelector.isVisible = false
        logToolBar.isVisible = false
        splitLogPane.fullLogPanel.isVisible = false
        configureContext(this)
        onSetup(Bundle().apply {
            put(LogMetadata.KEY, LogMetadataModel.default.toLogMetadata())
        })
    }

    fun setMetadata(metadataModel: LogMetadataEditModel) {
        val metadata = generateThemeIgnoredLogMetadata(metadataModel)
        logConf.setLogMetadata(metadata)
        startProduceLog(metadata)
        updateLogFilter()
    }

    /**
     * replace all [DarkThemeAwareColor] with a fixed color according to [LogMetadataEditModel.isNightMode]
     */
    @Suppress("UNCHECKED_CAST")
    private fun generateThemeIgnoredLogMetadata(editModel: LogMetadataEditModel): LogMetadata {
        fun generateThemeIgnoredColor(color: DarkThemeAwareColor, isNightMode: Boolean): DarkThemeAwareColor {
            val targetColor = if (isNightMode) color.nightColor else color.dayColor
            return DarkThemeAwareColor(targetColor, targetColor)
        }

        fun <T> replaceColorFieldsWithFixedColor(obj: Any, isNightMode: Boolean): T {
            obj.javaClass.declaredFields
                .filter { it.type == DarkThemeAwareColor::class.java && !Modifier.isStatic(it.modifiers) }
                .forEach {
                    it.isAccessible = true
                    val color = it.get(obj) as DarkThemeAwareColor
                    val newColor = generateThemeIgnoredColor(color, isNightMode)
                    it.set(obj, newColor)
                }
            return obj as T
        }

        val newMetadata = editModel.model.toLogMetadata().deepCopy()
        replaceColorFieldsWithFixedColor<LogColorScheme>(newMetadata.colorScheme, editModel.isNightMode)
        newMetadata.levels.forEach {
            replaceColorFieldsWithFixedColor<DisplayedLevel>(it, editModel.isNightMode)
        }
        return newMetadata
    }

    private fun startProduceLog(metadata: LogMetadata) {
        if (metadata.isDeviceLog) {
            logViewModel.startProduceCustomDeviceLog()
        } else {
            logViewModel.startProduceCustomFileLog()
        }
    }

    override fun getCustomToolbarComponents(): List<Component> {
        return emptyList()
    }

    override fun configureLogTablePopupActions() {
        // do nothing
    }
}
