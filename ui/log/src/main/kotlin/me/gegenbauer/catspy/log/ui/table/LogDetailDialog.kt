package me.gegenbauer.catspy.log.ui.table

import me.gegenbauer.catspy.configuration.currentSettings
import me.gegenbauer.catspy.log.metadata.LogMetadata
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.Key
import me.gegenbauer.catspy.utils.ui.installKeyStrokeEscClosing
import me.gegenbauer.catspy.utils.ui.keyEventInfo
import me.gegenbauer.catspy.view.panel.ScrollConstrainedScrollablePanel
import me.gegenbauer.catspy.view.panel.VerticalFlexibleWidthLayout
import java.awt.Dimension
import java.awt.event.*
import javax.swing.*
import javax.swing.text.JTextComponent

open class LogDetailDialog(
    parent: JFrame,
    private val textComponent: JTextComponent,
    private val popupActions: List<PopupAction> = emptyList(),
    logMetadata: LogMetadata
) : JFrame() {

    private val contentContainer = ScrollConstrainedScrollablePanel(false)
    private val scrollPane = JScrollPane(contentContainer)
    private val popupMenu = PopUpLogViewDialog()

    init {
        contentContainer.layout = VerticalFlexibleWidthLayout()
        contentContainer.add(textComponent)
        textComponent.isEditable = false
        textComponent.caret.isVisible = true

        textComponent.addKeyListener(KeyHandler())
        textComponent.addMouseListener(MouseHandler())
        textComponent.selectionColor = logMetadata.colorScheme.selectedLogBackground
        var width = parent.width - 100
        if (width < 960) {
            width = 960
        }
        textComponent.setSize(width, 100)
        textComponent.border = BorderFactory.createEmptyBorder(7, 7, 7, 7)
        textComponent.font = currentSettings.logSettings.font.nativeFont
        textComponent.minimumSize = Dimension(width, 100)
        textComponent.maximumSize = Dimension(width, Int.MAX_VALUE)
        var height = parent.height - 100
        if (height > textComponent.preferredSize.height) {
            height = textComponent.preferredSize.height + 6
        }
        scrollPane.preferredSize = Dimension(width, height)

        contentPane.add(scrollPane)
        pack()

        installKeyStrokeEscClosing(this)
    }

    override fun dispose() {
        super.dispose()
        removeAll()
        popupMenu.isVisible = false
        scrollPane.removeAll()
    }

    internal inner class KeyHandler : KeyAdapter() {
        override fun keyPressed(event: KeyEvent) {
            if (event.keyEventInfo == Key.ENTER) {
                textComponent.copy()
                dispose()
            }
        }
    }

    internal inner class PopUpLogViewDialog : JPopupMenu() {
        private val copyItem = JMenuItem(STRINGS.ui.copy)
        private val closeItem = JMenuItem(STRINGS.ui.close)
        private val actionHandler = ActionHandler()

        init {
            popupActions.forEach {
                val item = JMenuItem(it.action)
                item.addActionListener { _ ->
                    it.onActionSelected(textComponent.selectedText)
                }
                add(item)
            }
            addSeparator()
            add(copyItem)
            add(closeItem)
            copyItem.addActionListener(actionHandler)
            closeItem.addActionListener(actionHandler)
            addFocusListener(FocusHandler())
        }

        internal inner class ActionHandler : ActionListener {
            override fun actionPerformed(event: ActionEvent) {
                when (event.source) {
                    copyItem -> {
                        textComponent.copy()
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
                if (!this@LogDetailDialog.hasFocus()) {
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

    data class PopupAction(val action: String, val onActionSelected: (String) -> Unit)
}
