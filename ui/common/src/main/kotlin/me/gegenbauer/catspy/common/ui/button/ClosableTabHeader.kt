package me.gegenbauer.catspy.common.ui.button

import com.github.weisj.darklaf.ui.tabbedpane.DarkTabbedPaneUI
import me.gegenbauer.catspy.utils.isDoubleClick
import me.gegenbauer.catspy.utils.isLeftClick
import me.gegenbauer.catspy.utils.isSingleClick
import me.gegenbauer.catspy.utils.loadDarklafThemedIcon
import java.awt.*
import java.awt.event.*
import javax.swing.*

class ClosableTabHeader(
    tabName: String,
    private val parent: JTabbedPane,
    icon: Icon? = null,
    private val closeable: Boolean = true,
    private val editable: Boolean = false,
    private val tabTooltip: String? = "",
) : JPanel() {

    var onCloseClicked: (() -> Unit)? = null

    private val title = JLabel(tabName, icon, SwingConstants.LEFT)
    private val closeButton = JButton(closeIconNormal)
    private val editor = JTextField()
    private var titleLen = 0
    private var editorMinDimen: Dimension = Dimension(0, 0)

    init {
        initUI()

        registerEvent()
    }

    private fun initUI() {
        layout = FlowLayout(FlowLayout.LEFT, CLOSE_BUTTON_PADDING_LEFT, 0)
        toolTipText = tabTooltip
        isOpaque = false
        name = "Tab.header"
        closeButton.name = "Tab.close"
        closeButton.toolTipText = "Close Tab"
        closeButton.isBorderPainted = false
        closeButton.isOpaque = false
        closeButton.background = Color(0, 0, 0, 0)
        val d = Dimension(CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
        closeButton.preferredSize = d
        title.border = null
        title.isOpaque = false
        title.name = "Tab.name"
        title.toolTipText = tabTooltip
        editor.border = BorderFactory.createLineBorder(editor.foreground)
        editor.margin = Insets(0, 0, 0, 0)

        add(title)
        if (closeable) {
            add(closeButton)
        }
    }

    private fun registerEvent() {
        closeButton.addActionListener { closeTab() }
        closeButton.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent) {
                closeButton.icon = closeIconHover
            }

            override fun mouseExited(e: MouseEvent) {
                closeButton.icon = closeIconNormal
            }
        })

        title.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.isLeftClick && e.isSingleClick) {
                    parent.selectedIndex = parent.indexOfTabComponent(this@ClosableTabHeader)
                    parent.requestFocusInWindow()
                }
                val rect: Rectangle = parent.ui.getTabBounds(parent, parent.selectedIndex)
                if (e.isLeftClick && e.isDoubleClick) {
                    startEditing()
                } else if (!rect.contains(e.getPoint()) && editor.isVisible) {
                    renameTabTitle()
                }
            }

            override fun mouseEntered(e: MouseEvent?) {
                setTabHovered(true)
            }
        })

        editor.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent) {
                if (editor.isVisible) {
                    renameTabTitle()
                }
            }
        })
        editor.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_ENTER -> renameTabTitle()
                    KeyEvent.VK_ESCAPE -> cancelEditing()
                    else -> {
                        editor.preferredSize = if (editor.getText().length > titleLen) null else editorMinDimen
                        parent.revalidate()
                    }
                }
            }
        })

        if (editable) {
            parent.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "start-editing")
            parent.actionMap.put("start-editing", object : AbstractAction() {
                override fun actionPerformed(e: ActionEvent) {
                    startEditing()
                }
            })
        }
    }

    private fun closeTab() {
        val showConfirmDialog = JOptionPane.showConfirmDialog(
            parent, "Do you really want to close \"" + title.text + "\"?", "Are you sure?",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE
        )
        if (showConfirmDialog == JOptionPane.OK_OPTION) {
            onCloseClicked?.invoke()
        }
    }

    private fun startEditing() {
        if (!editable) {
            return
        }
        editor.text = title.text
        editor.selectAll()
        titleLen = editor.text.length
        editorMinDimen = editor.preferredSize
        editor.minimumSize = editorMinDimen

        remove(title)
        add(editor, 0)
        revalidate()

        editor.requestFocusInWindow()
    }

    private fun cancelEditing() {
        if (editor.isVisible) {
            remove(editor)
            add(title, 0)
            revalidate()
        }
    }

    private fun setTabHovered(isHovered: Boolean) {
        (parent.ui as? DarkTabbedPaneUI)?.setRolloverTab(if (isHovered) parent.indexOfTabComponent(this) else -1)
    }

    private fun renameTabTitle() {
        if (!editable) {
            return
        }
        val titleContent = editor.getText().trim()
        if (titleContent.isNotEmpty()) {
            title.text = titleContent
        }
        cancelEditing()
    }

    companion object {
        private const val CLOSE_BUTTON_SIZE = 20
        private const val CLOSE_BUTTON_PADDING_LEFT = 4
        private val closeIconNormal: Icon by lazy { loadDarklafThemedIcon("navigation/close.svg") }
        private val closeIconHover: Icon by lazy { loadDarklafThemedIcon("navigation/closeHovered.svg") }
    }
}