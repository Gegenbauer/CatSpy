package me.gegenbauer.catspy.log.ui.dialog

import me.gegenbauer.catspy.configuration.LogColorScheme
import me.gegenbauer.catspy.configuration.currentSettings
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.log.ui.panel.BaseLogMainPanel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.Key
import me.gegenbauer.catspy.utils.installKeyStrokeEscClosing
import me.gegenbauer.catspy.utils.keyEventInfo
import java.awt.Dimension
import java.awt.event.*
import javax.swing.*
import javax.swing.text.html.HTMLEditorKit


class LogViewDialog(
    parent: JFrame,
    log: String,
    override val contexts: Contexts = Contexts.default
) : JDialog(parent, false), Context {

    private val textArea = JTextPane()
    private val scrollPane = JScrollPane(textArea)
    private val popupMenu = PopUpLogViewDialog()

    init {
        isUndecorated = true
        textArea.isEditable = false
        textArea.caret.isVisible = true

        textArea.editorKit = HTMLEditorKit()
        textArea.addKeyListener(KeyHandler())
        textArea.addMouseListener(MouseHandler())
        textArea.addFocusListener(FocusHandler())
        textArea.text = log
        textArea.selectionColor = LogColorScheme.selectedBG
        var width = parent.width - 100
        if (width < 960) {
            width = 960
        }
        textArea.setSize(width, 100)
        textArea.border = BorderFactory.createEmptyBorder(7, 7, 7, 7)

        var height = parent.height - 100
        if (height > textArea.preferredSize.height) {
            height = textArea.preferredSize.height + 2
        }
        scrollPane.preferredSize = Dimension(width, height)

        contentPane.add(scrollPane)
        pack()

        installKeyStrokeEscClosing(this)
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        textArea.font = currentSettings.logSettings.font.toNativeFont()
    }

    internal inner class KeyHandler : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            if (event.keyEventInfo == Key.ENTER) {
                textArea.copy()
                destroy()
            }
        }
    }

    internal inner class FocusHandler : FocusAdapter() {
        override fun focusLost(event: FocusEvent) {
            super.focusLost(event)
            if (!popupMenu.isVisible) {
                destroy()
            }
        }
    }

    internal inner class PopUpLogViewDialog : JPopupMenu() {
        var includeItem = JMenuItem(STRINGS.ui.addInclude)
        var excludeItem = JMenuItem(STRINGS.ui.addExclude)
        var searchAddItem = JMenuItem(STRINGS.ui.addSearch)
        var searchSetItem = JMenuItem(STRINGS.ui.setSearch)
        var copyItem = JMenuItem(STRINGS.ui.copy)
        var closeItem = JMenuItem(STRINGS.ui.close)
        private val actionHandler = ActionHandler()

        init {
            includeItem.addActionListener(actionHandler)
            add(includeItem)
            excludeItem.addActionListener(actionHandler)
            add(excludeItem)
            searchAddItem.addActionListener(actionHandler)
            add(searchAddItem)
            searchSetItem.addActionListener(actionHandler)
            add(searchSetItem)
            copyItem.addActionListener(actionHandler)
            add(copyItem)
            closeItem.addActionListener(actionHandler)
            add(closeItem)
            addFocusListener(FocusHandler())
        }

        internal inner class ActionHandler : ActionListener {
            override fun actionPerformed(event: ActionEvent) {
                val logTabPanel = contexts.getContext(BaseLogMainPanel::class.java)
                logTabPanel ?: return
                val logMainBinding = logTabPanel.logMainBinding
                when (event.source) {
                    includeItem -> {
                        if (textArea.selectedText.isNullOrEmpty()) return
                        var text = logTabPanel.getTextShowLogCombo()
                        text += "|" + textArea.selectedText
                        logTabPanel.updateMessageFilter(text)
                    }

                    excludeItem -> {
                        if (textArea.selectedText.isNullOrEmpty()) return
                        var text = logTabPanel.getTextShowLogCombo()
                        text += "|-" + textArea.selectedText
                        logTabPanel.updateMessageFilter(text)
                    }

                    searchAddItem -> {
                        if (textArea.selectedText.isNullOrEmpty()) return
                        logMainBinding.searchPanelVisible.updateValue(true)
                        var text = logMainBinding.searchCurrentContent.getValueNonNull()
                        text += "|" + textArea.selectedText
                        logMainBinding.searchCurrentContent.updateValue(text)
                    }

                    searchSetItem -> {
                        logMainBinding.searchPanelVisible.updateValue(true)
                        logMainBinding.searchCurrentContent.updateValue(textArea.selectedText)
                    }

                    copyItem -> {
                        textArea.copy()
                    }

                    closeItem -> {
                        destroy()
                    }
                }
            }
        }

        internal inner class FocusHandler : FocusAdapter() {
            override fun focusLost(event: FocusEvent) {
                super.focusLost(event)
                if (!this@LogViewDialog.hasFocus()) {
                    destroy()
                }
            }
        }
    }

    internal inner class MouseHandler : MouseAdapter() {
        override fun mouseReleased(event: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(event)) {
                popupMenu.show(event.component, event.x, event.y)
            } else {
                popupMenu.isVisible = false
            }

            super.mouseReleased(event)
        }
    }

    override fun destroy() {
        super.destroy()
        dispose()
    }
}
