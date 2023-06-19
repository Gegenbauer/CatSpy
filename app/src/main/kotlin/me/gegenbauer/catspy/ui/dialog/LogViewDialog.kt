package me.gegenbauer.catspy.ui.dialog

import com.github.weisj.darklaf.ui.util.DarkUIUtil
import me.gegenbauer.catspy.resource.strings.STRINGS
import me.gegenbauer.catspy.ui.MainUI
import me.gegenbauer.catspy.ui.log.LogMainUI
import me.gegenbauer.catspy.utils.Utils
import me.gegenbauer.catspy.viewmodel.MainViewModel
import java.awt.Dimension
import java.awt.event.*
import javax.swing.*


class LogViewDialog(parent: JFrame, log: String, caretPos: Int) : JDialog(parent, false) {

    private val textArea = JTextArea()
    private val scrollPane = JScrollPane(textArea)
    private val mainUI = parent as MainUI
    private val popupMenu = PopUpLogViewDialog()

    init {
        isUndecorated = true
        textArea.isEditable = false
        textArea.caret.isVisible = true
        textArea.lineWrap = true

        val logMainUI = DarkUIUtil.getParentOfType(this, LogMainUI::class.java)
        textArea.font = logMainUI.customFont

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

    internal inner class KeyHandler : KeyAdapter() {
        private var pressedKeyCode: Int = 0
        override fun keyPressed(event: KeyEvent) {
            pressedKeyCode = event.keyCode
            super.keyPressed(event)
        }

        override fun keyReleased(event: KeyEvent) {
            if (event.keyCode == KeyEvent.VK_ENTER && pressedKeyCode == KeyEvent.VK_ENTER) {
                textArea.copy()
                dispose()
            }
        }
    }

    internal inner class FocusHandler : FocusAdapter() {
        override fun focusLost(event: FocusEvent) {
            super.focusLost(event)
            if (!popupMenu.isVisible) {
                dispose()
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
                val logMainUI = DarkUIUtil.getParentOfType(this@LogViewDialog, LogMainUI::class.java)
                when (event.source) {
                    includeItem -> {
                        if (textArea.selectedText.isNullOrEmpty()) return
                        var text = logMainUI.getTextShowLogCombo()
                        text += "|" + textArea.selectedText
                        logMainUI.setTextShowLogCombo(text)
                        logMainUI.applyShowLogCombo()
                    }

                    excludeItem -> {
                        if (textArea.selectedText.isNullOrEmpty()) return
                        var text = logMainUI.getTextShowLogCombo()
                        text += "|-" + textArea.selectedText
                        logMainUI.setTextShowLogCombo(text)
                        logMainUI.applyShowLogCombo()
                    }

                    searchAddItem -> {
                        if (textArea.selectedText.isNullOrEmpty()) return
                        var text = MainViewModel.searchCurrentContent.getValueNonNull()
                        text += "|" + textArea.selectedText
                        MainViewModel.searchCurrentContent.updateValue(text)
                    }

                    searchSetItem -> {
                        MainViewModel.searchPanelVisible.updateValue(true)
                        MainViewModel.searchCurrentContent.updateValue(textArea.selectedText)
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

        internal inner class FocusHandler : FocusAdapter() {
            override fun focusLost(event: FocusEvent) {
                super.focusLost(event)
                if (!this@LogViewDialog.hasFocus()) {
                    dispose()
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
}
