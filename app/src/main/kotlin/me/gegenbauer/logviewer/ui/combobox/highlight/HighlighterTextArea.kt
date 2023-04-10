package me.gegenbauer.logviewer.ui.combobox.highlight

import me.gegenbauer.logviewer.ui.combobox.FilterComboBox
import java.awt.Graphics
import java.awt.Insets
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JTextArea

class HighlighterTextArea : JTextArea(), Highlightable<HighlighterTextArea> {
    private val actionListeners = ArrayList<ActionListener>()
    private var prevCaret = 0
    private lateinit var combo: FilterComboBox
    private lateinit var textComponentWrapper: BaseHighlighterEditor<HighlighterTextArea>.HighlightTextComponentWrapper

    init {
        lineWrap = true

        margin = Insets(0, 0, 0, 0)

        addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) {
                // do nothing
            }

            override fun keyPressed(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_ENTER -> {
                        e.consume()
                    }

                    KeyEvent.VK_DOWN -> {
                        prevCaret = caretPosition
                        return
                    }

                    KeyEvent.VK_UP -> {
                        if (combo.isPopupVisible) {
                            e.consume()
                        }
                        return
                    }

                    KeyEvent.VK_TAB -> {
                        if (e.modifiersEx > 0) {
                            transferFocusBackward()
                        } else {
                            transferFocus()
                        }

                        e.consume()
                    }
                }

                setUpdateHighlighter(true)
            }

            override fun keyReleased(e: KeyEvent) {
                when (e.keyCode) {
                    KeyEvent.VK_ENTER -> {
                        e.consume()
                        for (listener in actionListeners) {
                            listener.actionPerformed(ActionEvent(this, ActionEvent.ACTION_PERFORMED, text))
                        }
                    }

                    KeyEvent.VK_DOWN -> {
                        if (prevCaret == caretPosition) {
                            e.consume()
                            if (!combo.isPopupVisible) {
                                combo.showPopup()
                            }
                            if (combo.selectedIndex < (combo.itemCount - 1)) {
                                combo.selectedIndex++
                            }
                        }
                        return
                    }

                    KeyEvent.VK_UP -> {
                        if (combo.isPopupVisible) {
                            e.consume()
                            if (combo.selectedIndex > 0) {
                                combo.selectedIndex--
                            }
                        }
                        return
                    }
                }
                combo.parent.revalidate()
                combo.parent.repaint()
            }
        })
    }

   override fun setTextComponentWrapper(textComponentWrapper: BaseHighlighterEditor<HighlighterTextArea>.HighlightTextComponentWrapper) {
        this.textComponentWrapper = textComponentWrapper
    }

    override fun setUpdateHighlighter(updateHighlighter: Boolean) {
        textComponentWrapper.setUpdateHighlighter(updateHighlighter)
    }

    override fun setEnableHighlighter(enable: Boolean) {
        textComponentWrapper.setEnableHighlighter(enable)
    }

    override fun paint(g: Graphics) {
        textComponentWrapper.paint()
        super.paint(g)
    }

    fun addActionListener(l: ActionListener) {
        actionListeners.add(l)
    }

    fun removeActionListener(l: ActionListener) {
        actionListeners.remove(l)
    }

    fun setComboBox(filterComboBox: FilterComboBox) {
        combo = filterComboBox
    }
}