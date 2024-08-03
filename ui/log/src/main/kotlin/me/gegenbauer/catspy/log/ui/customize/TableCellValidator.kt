package me.gegenbauer.catspy.log.ui.customize

import com.alexandriasoftware.swing.JInputValidator
import com.alexandriasoftware.swing.Validation
import com.alexandriasoftware.swing.border.ValidatorBorder
import java.awt.*
import javax.swing.JComponent
import javax.swing.border.AbstractBorder
import javax.swing.border.Border
import javax.swing.border.CompoundBorder
import kotlin.math.max

abstract class TableCellValidator(
    component: JComponent,
) : JInputValidator(component, true, true) {

    private val originalBorder = component.border

    override fun verify(input: JComponent): Boolean {
        val result = super.verify(input)
        val validation = getValidation(input, null)
        if (input.border is ValidatorBorder && validation.type != Validation.Type.NONE) {
            input.border = TableCellValidatorBorder(validation, originalBorder)
        }
        return result
    }
}

class TableCellValidatorBorder(
    private val validation: Validation,
    private val originalBorder: Border,
) : CompoundBorder() {

    private var font = validation.font.deriveFont(1, 0.0f)

    init {
        outsideBorder = originalBorder
        insideBorder = object : AbstractBorder() {
            override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
                val insets: Insets = originalBorder.getBorderInsets(c)

                val metrics: FontMetrics = getFontMetrics(c)
                val by = c.height / 2 + metrics.ascent / 2 - insets.top
                val bw = max(2.0, insets.right.toDouble()).toInt()
                val iw = metrics.stringWidth(validation.icon)
                val bx = x + width - Math.round(iw.toFloat() * 1.5f + bw.toFloat() * 1.5f) + 2
                g.translate(bx, by)
                g.color = validation.color
                g.font = font
                if (g is Graphics2D) {
                    g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
                    g.setRenderingHint(
                        RenderingHints.KEY_FRACTIONALMETRICS,
                        RenderingHints.VALUE_FRACTIONALMETRICS_ON
                    )
                }

                g.drawString(validation.icon, x + iw / 2 - 10, y)
            }

            override fun isBorderOpaque(): Boolean {
                return false
            }

            override fun getBorderInsets(c: Component, insets: Insets): Insets {
                val metrics: FontMetrics = getFontMetrics(c)
                val iw = metrics.stringWidth(validation.icon)
                insets.right = Math.round(iw.toFloat() * 1.5f + 10)
                return insets
            }
        }
    }

    private fun getFontMetrics(c: Component): FontMetrics {
        val cFont = c.font
        if (font.size != cFont.size) {
            font = validation.font.deriveFont(1, cFont.size.toFloat())
        }

        return c.getFontMetrics(this.font)
    }
}