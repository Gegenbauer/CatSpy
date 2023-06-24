package me.gegenbauer.catspy.ui.card

import java.awt.*
import javax.swing.BorderFactory
import javax.swing.JPanel

/**
 * 带圆角背景容器
 */
open class RoundedCard @JvmOverloads constructor(
    private val radius: Int = 30,
    private val background: Color = Color.WHITE,
    layout: LayoutManager = FlowLayout(),
    private val shadowSize: Int = 8
) : JPanel(layout) {
    private val padding = shadowSize + 8

    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(padding, padding, padding, padding)
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D

        // 绘制阴影
        g2.color = Color.BLACK
        g2.composite = AlphaComposite.SrcOver.derive(0.2f)
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.fillRoundRect(shadowSize / 2, shadowSize / 2, width - shadowSize, height - shadowSize, radius, radius)

        // 绘制卡片
        g2.color = background
        g2.composite = AlphaComposite.SrcOver
        g2.fillRoundRect(0, 0, width - shadowSize, height - shadowSize, radius, radius)

        super.paintComponent(g)
        g2.dispose()
    }
}