package me.gegenbauer.logviewer.ui.log

import me.gegenbauer.logviewer.manager.ConfigManager
import me.gegenbauer.logviewer.ui.MainUI
import me.gegenbauer.logviewer.strings.Strings
import me.gegenbauer.logviewer.Utils
import java.awt.Color
import java.awt.Dimension
import java.awt.event.*
import javax.swing.*


class LogViewDialog (parent: JFrame, log:String, caretPos: Int) : JDialog(parent, "Log", false) {

    val textArea = JTextArea()
    private val scrollPane = JScrollPane(textArea)
    private val mainUI = parent as MainUI
    private val popupMenu = PopUpLogViewDialog()

    init {
        isUndecorated = true
        textArea.isEditable = false
        textArea.caret.isVisible = true
        textArea.lineWrap = true
        if (ConfigManager.LaF != MainUI.FLAT_DARK_LAF) {
            textArea.background = Color(0xFF, 0xFA, 0xE3)
        }
        textArea.font = mainUI.customFont

        textArea.addKeyListener(KeyHandler())
        textArea.addMouseListener(MouseHandler())
        textArea.addFocusListener(FocusHandler())
        textArea.text = log
        textArea.caretPosition = caretPos
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

        Utils.installKeyStrokeEscClosing(this)
    }

    internal inner class KeyHandler: KeyAdapter() {
        private var pressedKeyCode: Int = 0
        override fun keyPressed(event: KeyEvent) {
            if (event != null) {
                pressedKeyCode = event.keyCode
            }

            super.keyPressed(event)
        }

        override fun keyReleased(event: KeyEvent) {
            if (event.keyCode == KeyEvent.VK_ENTER && pressedKeyCode == KeyEvent.VK_ENTER) {
                textArea.copy()
                dispose()
            }
        }
    }

    internal inner class FocusHandler: FocusAdapter() {
        override fun focusLost(event: FocusEvent) {
            super.focusLost(event)
            if (!popupMenu.isVisible) {
                dispose()
            }
        }
    }

    internal inner class PopUpLogViewDialog : JPopupMenu() {
        var includeItem = JMenuItem(Strings.ADD_INCLUDE)
        var excludeItem = JMenuItem(Strings.ADD_EXCLUDE)
        var searchAddItem = JMenuItem(Strings.ADD_SEARCH)
        var searchSetItem = JMenuItem(Strings.SET_SEARCH)
        var copyItem = JMenuItem(Strings.COPY)
        var closeItem = JMenuItem(Strings.CLOSE)
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
                when (event.source) {
                    includeItem -> {
                        var text = mainUI.getTextShowLogCombo()
                        text += "|" + textArea.selectedText
                        mainUI.setTextShowLogCombo(text)
                        mainUI.applyShowLogCombo()
                    }
                    excludeItem -> {
                        if (!textArea.selectedText.isNullOrEmpty()) {
                            var text = mainUI.getTextShowLogCombo()
                            text += "|-" + textArea.selectedText
                            mainUI.setTextShowLogCombo(text)
                            mainUI.applyShowLogCombo()
                        }
                    }
                    searchAddItem -> {
                        if (!textArea.selectedText.isNullOrEmpty()) {
                            var text = mainUI.getTextSearchCombo()
                            text += "|" + textArea.selectedText
                            mainUI.setTextSearchCombo(text)
                        }
                    }
                    searchSetItem -> {
                        if (!textArea.selectedText.isNullOrEmpty()) {
                            mainUI.setTextSearchCombo(textArea.selectedText)
                        }
                    }
                    copyItem -> {
                        textArea.copy()
                    }
                    closeItem -> {
                        dispose()
                    }
                }
            }
        }

        internal inner class FocusHandler: FocusAdapter() {
            override fun focusLost(event: FocusEvent) {
                super.focusLost(event)
                if (!this@LogViewDialog.hasFocus()) {
                    dispose()
                }
            }
        }
    }

    internal inner class MouseHandler : MouseAdapter() {
        override fun mousePressed(event: MouseEvent) {
            super.mousePressed(event)
        }

        override fun mouseReleased(event: MouseEvent) {
            if (SwingUtilities.isRightMouseButton(event)) {
                popupMenu.show(event.component, event.x, event.y)
            } else {
                popupMenu.isVisible = false
            }

            super.mouseReleased(event)
        }

    }
}
