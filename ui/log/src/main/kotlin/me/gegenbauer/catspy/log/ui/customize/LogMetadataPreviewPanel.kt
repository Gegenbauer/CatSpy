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

    private var logProducer: (() -> List<String>) = { emptyList() }

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
    }

    fun setLogProducer(producer: () -> List<String>) {
        logProducer = producer
        startProduceLog(logConf.logMetaData)
    }

    private fun startProduceLog(metadata: LogMetadata) {
        if (metadata.isDeviceLog) {
            logViewModel.startProduceCustomDeviceLog(logProducer)
        } else {
            logViewModel.startProduceCustomFileLog(logProducer)
        }
    }

    override fun getCustomToolbarComponents(): List<Component> {
        return emptyList()
    }

    override fun configureLogTablePopupActions() {
        // do nothing
    }
}
