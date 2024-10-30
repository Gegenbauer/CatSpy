package me.gegenbauer.catspy.log.ui.tab

import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants.FILL
import info.clearthought.layout.TableLayoutConstants.PREFERRED
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.java.ext.Bundle
import me.gegenbauer.catspy.view.panel.FileDropHandler
import me.gegenbauer.catspy.view.tab.BaseTabPanel
import me.gegenbauer.catspy.view.tab.TabPanel
import java.awt.CardLayout
import java.io.File
import javax.swing.JPanel

class FileLogCommonPanel : BaseTabPanel() {
    override val tag: String = TAG

    private val rootLayout = CardLayout()
    private val defaultPanel = JPanel()
    private val logGuidancePanel = FileLogGuidancePanel(::openFile)
    private val fileLogMainPanel = FileLogMainPanel()
    private val fileGroupMainPanel = FileGroupMainPanel()

    override fun onSetup(bundle: Bundle?) {
        createUI()

        registerEvent()
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        fileLogMainPanel.setParent(this)
        fileGroupMainPanel.setParent(this)
    }

    override fun isFileAcceptable(files: List<File>): Boolean {
        return Companion.isFileAcceptable(files)
    }

    override fun handleFileDrop(files: List<File>) {
        onOpenFileRequested(files)
    }

    private fun createUI() {
        defaultPanel.layout = TableLayout(
            doubleArrayOf(0.25, FILL, 0.25),
            doubleArrayOf(0.3, PREFERRED, 0.25)
        )
        defaultPanel.add(logGuidancePanel, "1,1")

        layout = rootLayout
        addPanel(defaultPanel)
        addPanel(fileLogMainPanel)
        addPanel(fileGroupMainPanel)

        showPanel(defaultPanel)
    }

    private fun addPanel(panel: JPanel) {
        add(panel, panel::class.java.name)
    }

    private fun showPanel(panel: JPanel) {
        rootLayout.show(this, panel::class.java.name)
        (panel as? TabPanel)?.setup(null)
    }

    private fun registerEvent() {

    }

    private fun openFile(file: File) {
        onOpenFileRequested(listOf(file))
    }

    override fun onOpenFileRequested(files: List<File>) {
        if (FileGroupMainPanel.isFileAcceptable(files)) {
            showPanel(fileGroupMainPanel)
            fileGroupMainPanel.handleFileDrop(files)
        } else if (FileLogMainPanel.isFileAcceptable(files)) {
            showPanel(fileLogMainPanel)
            fileLogMainPanel.handleFileDrop(files)
        }
    }

    companion object : FileDropHandler {
        private const val TAG = "FileLogCommonPanel"

        override fun isFileAcceptable(files: List<File>): Boolean {
            return FileGroupMainPanel.isFileAcceptable(files) || FileLogMainPanel.isFileAcceptable(files)
        }
    }
}