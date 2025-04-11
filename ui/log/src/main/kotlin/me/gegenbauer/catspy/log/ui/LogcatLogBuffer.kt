package me.gegenbauer.catspy.log.ui

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.log.filter.StorableValueProperty
import me.gegenbauer.catspy.log.ui.customize.CenteredDualDirectionPanel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.persistence.Preferences
import me.gegenbauer.catspy.utils.persistence.UserPreferences
import me.gegenbauer.catspy.utils.ui.applyTooltip
import me.gegenbauer.catspy.utils.ui.createSpace
import me.gegenbauer.catspy.utils.ui.findFrameFromParent
import me.gegenbauer.catspy.utils.ui.installKeyStrokeEscClosing
import me.gegenbauer.catspy.view.button.GButton
import me.gegenbauer.catspy.view.panel.VerticalFlexibleWidthLayout
import java.awt.BorderLayout
import java.awt.Window
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JDialog
import javax.swing.JPanel

enum class LogcatLogBuffer(val command: String) {
    MAIN("main"),
    SYSTEM("system"),
    RADIO("radio"),
    EVENTS("events"),
    CRASH("crash"),
    KERNEL("kernel"),
    SECURITY("security");

    fun stringToLogcatLogBuffer(value: String): LogcatLogBuffer {
        return valueOf(value.uppercase())
    }
}

fun List<String>.toLogcatLogBuffers(): List<LogcatLogBuffer> {
    return map { LogcatLogBuffer.valueOf(it.uppercase()) }
}

fun List<LogcatLogBuffer>.toLogcatLogBufferCommand(): String {
    return joinToString(" ") { it.command }
}

interface ILogcatLogBufferSelectPanel : Context {
    fun getLogBufferSelectPanel(): LogcatLogBufferSelectPanel

    fun getLogBuffers(): List<String>

    fun setLogBufferSelectorEnabled(enabled: Boolean)
}

class LogcatLogBufferSelectPanel(
    override val contexts: Contexts = Contexts.default
) : JPanel(), ILogcatLogBufferSelectPanel {
    private val selectBtn = JButton(STRINGS.ui.selectLogBuffer) applyTooltip STRINGS.toolTip.selectLogBuffer

    private val buffers: ObservableValueProperty<List<String>> =
        StorableValueProperty(KEY_SELECTED_BUFFERS, emptyList())
    private val selectedBufferChangeListener = object : UserPreferences.PreferencesChangeListener {
        override val key: String
            get() = KEY_SELECTED_BUFFERS

        override fun onPreferencesChanged() {
            updateTooltip()
        }
    }

    init {
        layout = BorderLayout()
        add(selectBtn, BorderLayout.CENTER)

        selectBtn.addActionListener {
            val dialog = LogcatLogBufferSelectDialog(findFrameFromParent(), buffers)
            dialog.isVisible = true
        }

        Preferences.addChangeListener(selectedBufferChangeListener)
        updateTooltip()
    }

    private fun updateTooltip() {
        selectBtn.toolTipText = buffers.value?.joinToString(", ") ?: ""
    }

    override fun getLogBufferSelectPanel(): LogcatLogBufferSelectPanel {
        return this
    }

    override fun getLogBuffers(): List<String> {
        return buffers.value ?: emptyList()
    }

    override fun setLogBufferSelectorEnabled(enabled: Boolean) {
        selectBtn.isEnabled = enabled
    }

    private class LogcatLogBufferSelectDialog(
        parent: Window,
        private val buffers: ObservableValueProperty<List<String>>
    ) : JDialog(parent) {
        private val selectButtons = LogcatLogBuffer.values().map { SelectButton(it, it.command) }

        private val selectAllBtn = JButton(STRINGS.ui.selectAll)
        private val clearBtn = JButton(STRINGS.ui.clear)

        private val okButton = GButton(STRINGS.ui.ok)
        private val cancelButton = GButton(STRINGS.ui.cancel)
        private val bottomPanel = CenteredDualDirectionPanel()

        init {
            modalityType = ModalityType.APPLICATION_MODAL
            title = STRINGS.ui.selectLogBuffer
            defaultCloseOperation = DISPOSE_ON_CLOSE
            installKeyStrokeEscClosing(this)

            getRootPane().defaultButton = okButton

            val selectPanel = createSelectPanel()

            val actionPanel = JPanel().apply {
                add(selectAllBtn)
                add(clearBtn)
            }
            bottomPanel.border = BorderFactory.createEmptyBorder(30, 4, 8, 6)
            bottomPanel.addRight(createSpace(100))
            bottomPanel.addRight(cancelButton)

            bottomPanel.addLeft(createSpace(100))
            bottomPanel.addLeft(okButton)

            contentPane.layout = VerticalFlexibleWidthLayout()
            contentPane.add(selectPanel)
            contentPane.add(actionPanel)
            contentPane.add(bottomPanel)
            pack()

            setLocationRelativeTo(parent)

            okButton.addActionListener {
                buffers.updateValue(getSelectedBuffers())
                isVisible = false
            }

            cancelButton.addActionListener {
                isVisible = false
            }

            clearBtn.addActionListener {
                selectBuffers(emptyList())
            }

            selectAllBtn.addActionListener {
                selectBuffers(LogcatLogBuffer.values().map { it.command })
            }

            selectBuffers(buffers.value ?: emptyList())
        }

        private fun createSelectPanel(): JPanel {
            return JPanel().apply {
                selectButtons.forEach {
                    add(it)
                }
            }
        }

        private fun selectBuffers(buffers: List<String>) {
            selectButtons.forEach {
                it.updateSelectState(buffers)
            }
        }

        private fun getSelectedBuffers(): List<String> {
            return selectButtons.filter { it.isSelected }.map { it.buffer.command }
        }

        private class SelectButton(val buffer: LogcatLogBuffer, title: String) : JCheckBox(title) {
            fun isSelected(selectedBuffers: List<String>): Boolean {
                return selectedBuffers.contains(buffer.command)
            }

            fun updateSelectState(selectedBuffers: List<String>) {
                isSelected = isSelected(selectedBuffers)
            }
        }
    }

    companion object {
        const val KEY_SELECTED_BUFFERS = "selected_logcat_log_buffers"
    }
}