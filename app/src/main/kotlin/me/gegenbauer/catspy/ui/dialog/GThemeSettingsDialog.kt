package me.gegenbauer.catspy.ui.dialog

import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.configuration.*
import me.gegenbauer.catspy.file.gson
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.concurrency.GlobalMessageManager
import me.gegenbauer.catspy.concurrency.Message
import me.gegenbauer.catspy.concurrency.UIScope
import me.gegenbauer.catspy.java.ext.copyFields
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.globalLocale
import me.gegenbauer.catspy.utils.ui.installKeyStrokeEscClosing
import me.gegenbauer.catspy.utils.ui.showWarningDialog
import me.gegenbauer.catspy.view.panel.ScrollConstrainedScrollablePanel
import java.awt.*
import java.awt.datatransfer.StringSelection
import javax.swing.*

class GThemeSettingsDialog(
    parent: Window,
    private val selectedGroupIndex: Int = GROUP_INDEX_APPEARANCE
) : JDialog(parent), SettingsContainer {

    private var tree = SettingsTree()

    private var startSettings = SettingsManager.string
    private val scope = UIScope()

    init {
        title = STRINGS.ui.preferences

        initUI()

        defaultCloseOperation = DISPOSE_ON_CLOSE
        installKeyStrokeEscClosing(this)
        modalityType = ModalityType.APPLICATION_MODAL
        pack()

        setLocationRelativeTo(null)
    }

    private fun initUI() {
        val wrapGroupPanel = ScrollConstrainedScrollablePanel(false)
        wrapGroupPanel.layout = BorderLayout(10, 10)
        val groups = ArrayList<ISettingsGroup>()
        groups.add(AppearanceSettingsGroup(scope, this))
        groups.add(AdbSettingsGroup(scope, this))

        tree = SettingsTree()
        tree.init(wrapGroupPanel, groups, selectedGroupIndex)

        val rightPane = JScrollPane(wrapGroupPanel)
        rightPane.verticalScrollBar.unitIncrement = 16
        rightPane.border = BorderFactory.createEmptyBorder(10, 3, 3, 10)

        val leftPane = JScrollPane(tree)
        leftPane.border = BorderFactory.createEmptyBorder(10, 10, 3, 3)

        val splitPane = JSplitPane()
        splitPane.resizeWeight = 0.2
        splitPane.leftComponent = leftPane
        splitPane.rightComponent = rightPane

        val panel = JPanel(BorderLayout())
        panel.add(splitPane, BorderLayout.CENTER)
        panel.add(buildButtonsPane(), BorderLayout.PAGE_END)
        contentPane.add(panel)

        currentSettings.windowSettings.loadWindowSettings(
            this,
            Rectangle(
                (parent.bounds.x + parent.bounds.width / 2 - DEFAULT_WINDOW_WIDTH / 2),
                (parent.bounds.y + parent.bounds.height / 2 - DEFAULT_WINDOW_HEIGHT / 2),
                DEFAULT_WINDOW_WIDTH,
                DEFAULT_WINDOW_HEIGHT
            )
        )

        installKeyStrokeEscClosing(this) { cancel() }
    }

    private fun buildButtonsPane(): JPanel {
        val saveBtn = JButton(STRINGS.ui.save)
        saveBtn.addActionListener { save() }

        val cancelButton = JButton(STRINGS.ui.cancel)
        cancelButton.addActionListener { cancel() }

        val resetBtn = JButton(STRINGS.ui.reset)
        resetBtn.addActionListener { reset() }

        val resetToDefault = JButton(STRINGS.ui.resetToDefaultTheme)
        resetToDefault.addActionListener { resetToDefault() }

        val copyBtn = JButton(STRINGS.ui.copyToClipboard)
        copyBtn.addActionListener { copySettings() }

        val buttonPane = JPanel()
        buttonPane.layout = BoxLayout(buttonPane, BoxLayout.LINE_AXIS)
        buttonPane.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        buttonPane.add(resetBtn)
        buttonPane.add(resetToDefault)
        buttonPane.add(copyBtn)
        buttonPane.add(Box.createHorizontalGlue())
        buttonPane.add(saveBtn)
        buttonPane.add(Box.createRigidArea(Dimension(10, 0)))
        buttonPane.add(cancelButton)

        getRootPane().defaultButton = saveBtn
        return buttonPane
    }

    private fun save() {
        checkAndApplyLanguage()
        dispose()
        startSettings = SettingsManager.string
    }

    private fun checkAndApplyLanguage() {
        if (globalLocale.ordinal == currentSettings.mainUISettings.locale) {
            return
        }
        if (showApplyLanguageWarning()) {
            SettingsManager.updateLocale()
        }
    }

    private fun showApplyLanguageWarning(): Boolean {
        val actions = listOf(
            STRINGS.ui.restartLater to { false },
            STRINGS.ui.restartNow to { true }
        )
        return showWarningDialog(
            this,
            EMPTY_STRING,
            STRINGS.ui.applyLanguageWarning,
            actions,
            defaultChoice = 1
        )
    }

    private fun reset() {
        scope.launch { suspendedReset() }
    }

    private fun resetToDefault() {
        if (showResetToDefaultWarning()) {
            scope.launch { suspendedResetToDefault() }
        }
    }

    private fun showResetToDefaultWarning(): Boolean {
        val actions = listOf(
            STRINGS.ui.reset to { true },
            STRINGS.ui.cancel to { false }
        )
        return showWarningDialog(
            this,
            EMPTY_STRING,
            STRINGS.ui.resetToDefaultWarning,
            actions,
        )
    }

    private suspend fun suspendedReset() {
        resetSettings()
        reloadUI()
    }

    private suspend fun suspendedResetToDefault() {
        SettingsManager.suspendedUpdateSettings {
            themeSettings.resetToDefault()
            mainUISettings.resetToDefault()
            logSettings.resetToDefault()
        }
        reloadUI()
    }

    private suspend fun resetSettings() {
        SettingsManager.suspendedUpdateSettings {
            val originalSettings = gson.fromJson(startSettings, GSettings::class.java)
            copyFields(originalSettings, currentSettings)
        }
    }

    override fun reloadUI() {
        val selection = tree.selectionRows
        contentPane.removeAll()
        initUI()
        tree.selectionRows = selection
        SwingUtilities.updateComponentTreeUI(this)
    }

    private fun cancel() {
        scope.launch {
            resetSettings()
            dispose()
        }
    }

    override fun dispose() {
        super.dispose()
        SettingsManager.updateSettings {
            windowSettings.saveWindowSettings(this@GThemeSettingsDialog)
        }
        scope.cancel()
    }

    private fun copySettings() {
        val settings = currentSettings
        val json = gson.toJson(settings)
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(StringSelection(json), null)
        GlobalMessageManager.publish(Message.Info(STRINGS.ui.settingsCopyToClipboardSuccess))
    }

    companion object {
        const val GROUP_INDEX_APPEARANCE = 1
        const val GROUP_INDEX_ADB = 2

        private const val DEFAULT_WINDOW_WIDTH = 1000
        private const val DEFAULT_WINDOW_HEIGHT = 600
    }
}