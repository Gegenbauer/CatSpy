package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.strings.STRINGS
import java.awt.Dimension
import javax.swing.JTextField

open class WidthConstrainedTextField(
    text: String = EMPTY_STRING,
    private val tooltip: String? = null,
    private val maxCharCount: Int = Int.MAX_VALUE
) : JTextField(text) {

    init {
        toolTipText = getTooltip()
    }

    private fun getTooltip(): String {
        return if (isEditable && tooltip?.isNotEmpty() == true) {
            "$tooltip\n${STRINGS.toolTip.paramEditorConfirmHint}"
        } else {
            tooltip ?: EMPTY_STRING
        }
    }

    override fun setEditable(b: Boolean) {
        super.setEditable(b)
        toolTipText = getTooltip()
    }

    constructor(tooltip: String?, maxCharCount: Int) : this(EMPTY_STRING, tooltip, maxCharCount)

    override fun getMaximumSize(): Dimension {
        if (text.length > maxCharCount) return Dimension(getMaxWidth(), super.getMaximumSize().height)
        return super.getMaximumSize()
    }

    override fun getPreferredSize(): Dimension {
        if (text.length > maxCharCount) return Dimension(getMaxWidth(), super.getPreferredSize().height)
        return super.getPreferredSize()
    }

    override fun setText(t: String?) {
        super.setText(t)
        caretPosition = 0
    }

    private fun getMaxWidth(): Int {
        val metrics = getFontMetrics(font)
        val maxCharWidth = metrics.charWidth('W')
        val charCount = text.length.coerceAtMost(maxCharCount)
        return maxCharWidth * charCount
    }
}