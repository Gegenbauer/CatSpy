package me.gegenbauer.catspy.log.ui.customize

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.log.metadata.LogMetadataManager
import me.gegenbauer.catspy.log.serialize.toLogMetadataModel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.installKeyStrokeEscClosing
import me.gegenbauer.catspy.utils.ui.setSizePercentage
import java.awt.Frame
import javax.swing.JDialog
import javax.swing.JSplitPane
import javax.swing.WindowConstants

class LogMetadataDialog(parent: Frame): JDialog(parent) {

    private val scope = MainScope()

    private val metadataManager: LogMetadataManager
        get() = ServiceManager.getContextService(LogMetadataManager::class.java)

    private val listPanel = LogMetadataListPanel()
    private val detailPanel = LogMetadataDetailPanel()

    init {
        initUI()

        loadLogMetadata()
    }

    private fun initUI() {
        title = STRINGS.ui.logMetadataEditorTitle

        val splitPane = JSplitPane()
        setSizePercentage(splitPane, 90, 70)
        splitPane.resizeWeight = 0.06
        splitPane.leftComponent = listPanel
        splitPane.rightComponent = detailPanel
        contentPane.add(splitPane)

        listPanel.bind(detailPanel)

        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        modalityType = ModalityType.MODELESS
        pack()

        setLocationRelativeTo(null)
    }

    private fun loadLogMetadata() {
        scope.launch {
            val logMetadataList = metadataManager.loadAllMetadata().map { it.toLogMetadataModel() }
            listPanel.data = logMetadataList
        }
    }
}