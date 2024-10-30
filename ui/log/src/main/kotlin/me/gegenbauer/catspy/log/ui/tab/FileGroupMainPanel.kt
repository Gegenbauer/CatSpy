package me.gegenbauer.catspy.log.ui.tab

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.file.archiveExtensions
import me.gegenbauer.catspy.file.isSingleFileArchive
import me.gegenbauer.catspy.java.ext.Bundle
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.Log
import me.gegenbauer.catspy.log.datasource.FileGroupSearchViewModel
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.log.metadata.LogMetadataManager
import me.gegenbauer.catspy.log.serialize.toLogMetadata
import me.gegenbauer.catspy.log.ui.LogConfiguration
import me.gegenbauer.catspy.log.ui.customize.CenteredDualDirectionPanel
import me.gegenbauer.catspy.log.ui.table.FilteredLogTableModel
import me.gegenbauer.catspy.log.ui.table.LogPanel
import me.gegenbauer.catspy.view.panel.FileDropHandler
import me.gegenbauer.catspy.view.panel.VerticalFlexibleWidthLayout
import me.gegenbauer.catspy.view.tab.BaseTabPanel
import java.awt.BorderLayout
import java.io.File
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * View archive of log files or log files in a directory.
 */
class FileGroupMainPanel : BaseTabPanel() {
    override val tag: String = TAG

    private val logToolBar = CenteredDualDirectionPanel(4)
    private val startSearchButton = JButton("Start Search")
    private val stopSearchButton = JButton("Stop Search")
    private val targetFileLabel = JLabel("Target File")
    private val targetFileValue = JLabel()
    private val topPanel = JPanel(VerticalFlexibleWidthLayout(5))

    private val logConf: LogConfiguration = LogConfiguration()
    private val metadata: LogMetadata by lazy {
        val logMetadataManager = ServiceManager.getContextService(LogMetadataManager::class.java)
        logMetadataManager.getMetadata(LogMetadataManager.LOG_TYPE_FILE_GROUP_SEARCH_RESULT)?.toLogMetadata()
            ?: error("LogMetadataModel for ${LogMetadataManager.LOG_TYPE_RAW} not found")
    }
    private val logViewModel = FileGroupSearchViewModel()
    private val logTableModel = FilteredLogTableModel(logViewModel)
    private val logPanel = LogPanel(logTableModel)

    override fun onSetup(bundle: Bundle?) {
        configureLogMetadata()

        createUI()

        registerEvent()
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        contexts.putContext(logConf)
        ServiceManager.getContextService(this, BookmarkManager::class.java)
        logConf.setParent(this)
    }

    private fun createUI() {
        layout = BorderLayout()

        logToolBar.addLeft(targetFileLabel)
        logToolBar.addLeft(targetFileValue)
        logToolBar.addRight(stopSearchButton)
        logToolBar.addRight(startSearchButton)

        topPanel.add(logToolBar)
        topPanel.add(logConf.filterPanel)
        topPanel.add(logConf.getSearchPanel())

        add(topPanel, BorderLayout.NORTH)
        add(logPanel, BorderLayout.CENTER)
    }

    private fun configureLogMetadata() {
        logConf.setLogMetadata(metadata)
    }

    private fun registerEvent() {
        // no-op
    }

    override fun handleFileDrop(files: List<File>) {
        val file = files.first()
        targetFileValue.text = file.absolutePath
        // TODO: Implement
        Log.d(TAG, "[handleFileDrop] file=$file")
    }

    companion object: FileDropHandler {
        private const val TAG = "FileGroupMainPanel"

        override fun isFileAcceptable(files: List<File>): Boolean {
            if (files.size != 1) return false
            val file = files.first()
            return if (file.isDirectory) {
                (file.listFiles()?.size ?: 0) > 0
            } else {
                ((file.extension in archiveExtensions) && !isSingleFileArchive(file))
            }
        }
    }
}