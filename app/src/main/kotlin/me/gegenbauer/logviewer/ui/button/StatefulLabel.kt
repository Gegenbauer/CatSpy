package me.gegenbauer.logviewer.ui.button

import me.gegenbauer.logviewer.databinding.bind.componentName
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.plaf.LabelUI

class StatefulLabel(
    private val originalIcon: Icon? = null,
    private val originalText: String? = null
) : JLabel(originalText), StatefulActionComponent {
    override var buttonDisplayMode: ButtonDisplayMode? = ButtonDisplayMode.ALL
        set(value) {
            field = value
            setDisplayMode(value)
        }

    override fun setDisplayMode(mode: ButtonDisplayMode?) {
        when (mode) {
            ButtonDisplayMode.TEXT -> {
                text = originalText
                icon = null
            }

            ButtonDisplayMode.ICON -> {
                text = null
                icon = originalIcon
            }

            else -> {
                text = originalText
                icon = originalIcon
            }
        }
    }

    init {
        componentName = originalText ?: ""
    }

    override fun setUI(ui: LabelUI?) {
        super.setUI(ui)
        setDisplayMode(buttonDisplayMode)
    }
}