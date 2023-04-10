package me.gegenbauer.logviewer.ui.combobox.highlight

import me.gegenbauer.logviewer.manager.ColorManager
import me.gegenbauer.logviewer.manager.ConfigManager
import me.gegenbauer.logviewer.ui.MainUI
import java.awt.Color
import java.awt.Component
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JComboBox
import javax.swing.text.DefaultHighlighter
import javax.swing.text.JTextComponent

abstract class BaseHighlighterEditor<T : JTextComponent>(protected val textComponent: T, protected val comboBox: JComboBox<*>) : HighlighterEditor() {
    protected val textComponentWrapper = HighlightTextComponentWrapper(textComponent)

    override fun getEditorComponent(): Component {
        return textComponent
    }

    override fun setItem(item: Any?) {
        if (item is String) {
            textComponent.text = item
            textComponentWrapper.setUpdateHighlighter(true)
        } else {
            textComponent.text = ""
        }
    }

    override fun getItem(): Any {
        return textComponent.text
    }

    override fun selectAll() {
        textComponent.selectAll()
    }

    inner class HighlightTextComponentWrapper(private val textComponent: T) {
        private var enableHighlighter = false
        private var updateHighlighter = false
        private val fgColor = textComponent.foreground

        init {
            (textComponent.highlighter as DefaultHighlighter).drawsLayeredHighlights = false

            textComponent.addFocusListener(object : FocusListener {
                override fun focusGained(e: FocusEvent) {
                    // do nothing
                }

                override fun focusLost(e: FocusEvent) {
                    println("focusLost")
                    setUpdateHighlighter(true)
                }
            })

            val colorEventListener = object : ColorManager.ColorEventListener {
                override fun colorChanged(event: ColorManager.ColorEvent) {
                    setUpdateHighlighter(true)
                    comboBox.repaint()
                }
            }

            ColorManager.addFilterStyleEventListener(colorEventListener)
            ColorManager.addColorEventListener(colorEventListener)
        }

        fun setUpdateHighlighter(updateHighlighter: Boolean) {
            this.updateHighlighter = updateHighlighter
            textComponent.repaint()
        }

        fun setEnableHighlighter(enable: Boolean) {
            enableHighlighter = enable
            if (enableHighlighter) {
                setUpdateHighlighter(true)
                textComponent.repaint()
            }
        }

        fun paint() {
            textComponent.foreground = if (errorMsg.isNotEmpty()) {
                if (ConfigManager.LaF == MainUI.FLAT_DARK_LAF) {
                    Color(0xC0, 0x70, 0x70)
                } else {
                    Color(0xFF, 0x00, 0x00)
                }
            } else {
                fgColor
            }

            if (enableHighlighter && updateHighlighter) {
                updateHighlighter(textComponent)
                updateHighlighter = false
            }
        }
    }
}