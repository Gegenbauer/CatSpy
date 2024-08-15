package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.java.ext.Bundle
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.serialize.LogMetadataModel
import me.gegenbauer.catspy.log.serialize.toLogMetadata
import me.gegenbauer.catspy.log.ui.tab.FileLogMainPanel
import java.awt.Component

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

    fun setMetadata(metadataModel: LogMetadataModel) {
        val metadata = metadataModel.toLogMetadata()
        logConf.setLogMetadata(metadata)
        startProduceLog(metadata)
        updateLogFilter()
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
