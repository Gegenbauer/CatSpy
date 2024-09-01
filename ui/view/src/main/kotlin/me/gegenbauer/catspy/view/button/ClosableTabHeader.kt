package me.gegenbauer.catspy.view.button

import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import me.gegenbauer.catspy.utils.ui.*
import me.gegenbauer.catspy.view.label.EllipsisLabel
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Insets
import java.awt.event.*
import javax.swing.*

class ClosableTabHeader(
    private val tabName: String,
    private val parent: JTabbedPane,
    icon: Icon? = null,
    private val closeable: Boolean = true,
    private val editable: Boolean = false,
    private val tabTooltip: String? = EMPTY_STRING,
) : JPanel() {

    var onCloseClicked: (() -> Unit)? = null

    private val title = EllipsisLabel(tabName, false, icon, 250)
    private val closeButton = CloseButton(::closeTab)
    private val editor = JTextField()
    private var titleLen = 0
    private var editorMinDimen: Dimension = Dimension(0, 0)

    private val clickListener = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
            if (e.isLeftClick && e.isSingleClick) {
                parent.selectedIndex = parent.indexOfTabComponent(this@ClosableTabHeader)
            }
            val rect = parent.ui.getTabBounds(parent, parent.selectedIndex)
            if (e.isLeftClick && e.isDoubleClick) {
                startEditing()
            } else if (!rect.contains(e.point) && editor.isVisible) {
                renameTabTitle()
            }
        }
    }

    init {
        initUI()

        registerEvent()
    }

    private fun initUI() {
        layout = FlowLayout(FlowLayout.LEFT, CLOSE_BUTTON_PADDING_LEFT, 0)
        isOpaque = false
        name = TAB_HEADER_KEY
        closeButton.name = TAB_CLOSE_KEY
        closeButton.toolTipText = STRINGS.toolTip.tabCloseBtn
        title.border = null
        title.isOpaque = false
        title.name = TAB_NAME_KEY
        editor.border = BorderFactory.createLineBorder(editor.foreground)
        editor.margin = Insets(0, 0, 0, 0)
        setTooltipInternal(tabTooltip ?: EMPTY_STRING)

        add(title)
        if (closeable) {
            add(closeButton)
        }
    }

    fun setTabTooltip(tooltip: String) {
        if (tooltip.isNotEmpty()) {
            setTooltipInternal(tooltip)
        } else {
            setTooltipInternal(tabTooltip ?: EMPTY_STRING)
        }
    }

    private fun setTooltipInternal(tooltip: String) {
        toolTipText = tooltip
        title.toolTipText = tooltip
    }

    fun setTabName(name: String) {
        title.text = name.ifEmpty { tabName }
        revalidate()
        repaint()
    }

    private fun registerEvent() {
        addMouseListener(clickListener)
        title.addMouseListener(clickListener)
        editor.addMouseListener(clickListener)

        editor.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent) {
                if (editor.isVisible) {
                    renameTabTitle()
                }
            }
        })
        editor.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                when (e.keyEventInfo) {
                    Key.ENTER -> renameTabTitle()
                    Key.ESCAPE -> cancelEditing()
                    else -> {
                        editor.preferredSize = if (editor.text.length > titleLen) null else editorMinDimen
                        parent.revalidate()
                    }
                }
            }
        })

        if (editable) {
            parent.registerStrokeWhenFocused(Key.ENTER, "Rename Tab") { startEditing() }
        }
    }

    private fun closeTab() {
        val showConfirmDialog = JOptionPane.showConfirmDialog(
            parent, STRINGS.ui.closeTabConfirmMessage.get(title.text),
            STRINGS.ui.closeTabConfirmTitle,
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE
        )
        if (showConfirmDialog == JOptionPane.OK_OPTION) {
            onCloseClicked?.invoke()
        }
    }

    private fun startEditing() {
        takeIf { editable } ?: return

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

    private fun renameTabTitle() {
        takeIf { editable } ?: return

        val titleContent = editor.text.trim()
        if (titleContent.isNotEmpty()) {
            title.text = titleContent
        }
        cancelEditing()
    }

    companion object {
        private const val CLOSE_BUTTON_PADDING_LEFT = 4

        private const val TAB_HEADER_KEY = "Tab"
        private const val TAB_NAME_KEY = "Tab.name"
        private const val TAB_CLOSE_KEY = "Tab.close"
        private const val TAB_NAME_MAX_WIDTH = Int.MAX_VALUE
    }
}