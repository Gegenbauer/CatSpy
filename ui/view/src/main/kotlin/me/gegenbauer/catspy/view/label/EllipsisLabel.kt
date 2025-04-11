package me.gegenbauer.catspy.view.label

import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import java.awt.Dimension
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import javax.swing.Icon
import javax.swing.JLabel
import kotlin.math.min

class EllipsisLabel(
    text: String = EMPTY_STRING,
    private val ellipsisAtEnd: Boolean = true,
    icon: Icon? = null,
    private val maxWidth: Int = Int.MAX_VALUE
) : JLabel(text) {

    private var displayText: String = text
    private var measureInfo: MeasureInfo = MeasureInfo(font, text, width)

    init {
        icon?.let { setIcon(it) }
        updateDisplayText(text)
    }

    constructor(
        text: String = EMPTY_STRING,
        icon: Icon? = null,
        maxWidth: Int = Int.MAX_VALUE
    ) : this(text, true, icon, maxWidth)

    override fun paintComponent(g: Graphics) {
        paintIcon(g)
        paintText(g)
    }

    private fun paintIcon(g: Graphics) {
        icon?.let {
            val x = insets.left
            val y = (height - it.iconHeight) / 2
            it.paintIcon(this, g, x, y)
        }
    }

    private fun paintText(g: Graphics) {
        if (measureInfo.hasChanged(font, text, width)) {
            if (updateDisplayText(text)) {
                measureInfo = MeasureInfo(font, text, width)
            }
        }
        val fm: FontMetrics = getFontMetrics(font)
        val x = insets.left + (icon?.iconWidth ?: 0) + iconTextGap
        val y = insets.top + fm.ascent
        g.drawString(displayText, x, y)
    }

    private fun updateDisplayText(text: String): Boolean {
        font ?: return false
        if (width == 0) return false
        val fm: FontMetrics = getFontMetrics(font)
        val iconWidth = icon?.iconWidth ?: 0
        val iconTextGap = iconTextGap.takeIf { icon != null } ?: 0
        val availableWidth = min(width - insets.left - insets.right - iconWidth - iconTextGap, maxWidth)
        displayText = text

        if (fm.stringWidth(text) > availableWidth) {
            val ellipsis = "..."
            var maxLength = text.length

            if (ellipsisAtEnd) {
                while (fm.stringWidth(text.substring(0, maxLength) + ellipsis) > availableWidth && maxLength > 0) {
                    maxLength--
                }
                displayText = text.substring(0, maxLength) + ellipsis
            } else {
                while (fm.stringWidth(ellipsis + text.substring(text.length - maxLength)) > availableWidth && maxLength > 0) {
                    maxLength--
                }
                displayText = ellipsis + text.substring(text.length - maxLength)
            }
        }
        return true
    }

    override fun getMaximumSize(): Dimension {
        val size = super.getMaximumSize()
        size.width = maxWidth
        return size
    }

    override fun getPreferredSize(): Dimension {
        val size = super.getPreferredSize()
        size.width = min(size.width + 2, maxWidth)
        return size
    }

    data class MeasureInfo(
        val font: Font?,
        val text: String,
        val width: Int
    ) {
        fun hasChanged(font: Font?, text: String, width: Int): Boolean {
            return this.font != font || this.text != text || this.width != width
        }
    }
}