package me.gegenbauer.catspy.view.hint

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.ui.FlatDropShadowBorder
import com.formdev.flatlaf.ui.FlatEmptyBorder
import com.formdev.flatlaf.ui.FlatUIUtils
import com.formdev.flatlaf.util.UIScale
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.configuration.currentSettings
import net.miginfocom.swing.MigLayout
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.geom.Area
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.border.Border

object HintManager {
    private val hintPanels = mutableListOf<HintPanel>()

    fun showHint(hint: Hint?) {
        if (hint == null) {
            return
        }
        if (currentSettings.isHintShown(hint.key)) {
            hint.nextHint?.let { showHint(it.nextHint) }
            return
        }

        val hintPanel = HintPanel(hint)
        hintPanel.showHint()

        hintPanels.add(hintPanel)
    }

    fun hideAllHints() {
        val hintPanelsCopy = hintPanels.toList()
        for (hintPanel in hintPanelsCopy) {
            hintPanel.hideHint()
        }
    }

    class Hint(
        val message: String,
        val owner: Component,
        val position: Int,
        val key: String,
        var nextHint: Hint? = null
    )

    private class HintPanel(private val hint: Hint) : JPanel() {
        private val hintLabel = JLabel()
        private val gotItButton = JButton("Got it!").apply {
            isFocusable = false
            addActionListener { gotIt() }
        }

        private var popup: JPanel? = null

        init {
            initComponents()
            isOpaque = false
            updateBalloonBorder()

            hintLabel.text = "<html>${hint.message}</html>"

            addMouseListener(object : MouseAdapter() {})
        }

        fun showHint() {
            val rootPane = SwingUtilities.getRootPane(hint.owner) ?: return
            val layeredPane = rootPane.layeredPane

            popup = object : JPanel(BorderLayout()) {
                override fun updateUI() {
                    super.updateUI()
                    validate()
                    size = preferredSize
                }
            }.apply {
                isOpaque = false
                add(this@HintPanel)
            }

            val pt = SwingUtilities.convertPoint(hint.owner, 0, 0, layeredPane)
            var x = pt.x
            var y = pt.y
            val size = popup!!.preferredSize
            val gap = UIScale.scale(6)

            when (hint.position) {
                SwingConstants.LEFT -> x -= size.width + gap
                SwingConstants.TOP -> y -= size.height + gap
                SwingConstants.RIGHT -> x += hint.owner.width + gap
                SwingConstants.BOTTOM -> y += hint.owner.height + gap
            }

            popup!!.setBounds(x, y, size.width, size.height)
            layeredPane.add(popup, JLayeredPane.POPUP_LAYER)
        }

        fun hideHint() {
            popup?.let {
                it.parent?.remove(it)
                it.parent?.revalidate()
            }

            hintPanels.remove(this)
        }

        override fun updateUI() {
            super.updateUI()

            background = if (UIManager.getLookAndFeel() is FlatLaf) {
                UIManager.getColor("HintPanel.backgroundColor")
            } else {
                // using nonUIResource() because otherwise Nimbus does not fill the background
                FlatUIUtils.nonUIResource(UIManager.getColor("info"))
            }

            if (hint != null) updateBalloonBorder()
        }

        private fun updateBalloonBorder() {
            val direction = when (hint.position) {
                SwingConstants.LEFT -> SwingConstants.RIGHT
                SwingConstants.TOP -> SwingConstants.BOTTOM
                SwingConstants.RIGHT -> SwingConstants.LEFT
                SwingConstants.BOTTOM -> SwingConstants.TOP
                else -> throw IllegalArgumentException()
            }
            border = BalloonBorder(direction, FlatUIUtils.getUIColor("PopupMenu.borderColor", Color.gray))
        }

        private fun gotIt() {
            hideHint()
            SettingsManager.updateSettings {
                currentSettings.setHintShown(hint.key)
            }
            hint.nextHint?.let { showHint(it) }
        }

        private fun initComponents() {
            layout = MigLayout("insets dialog,hidemode 3", "[::200,fill]", "[]para[]")
            add(hintLabel, "cell 0 0")
            add(gotItButton, "cell 0 1,alignx right,growx 0")
        }
    }

    private class BalloonBorder(private val direction: Int, private val borderColor: Color) : FlatEmptyBorder(1 + SHADOW_TOP_SIZE, 1 + SHADOW_SIZE, 1 + SHADOW_SIZE, 1 + SHADOW_SIZE) {
        private val shadowBorder: Border? = if (UIManager.getLookAndFeel() is FlatLaf) {
            FlatDropShadowBorder(
                UIManager.getColor("Popup.dropShadowColor"),
                Insets(SHADOW_SIZE2, SHADOW_SIZE2, SHADOW_SIZE2, SHADOW_SIZE2),
                FlatUIUtils.getUIFloat("Popup.dropShadowOpacity", 0.5f)
            )
        } else {
            null
        }

        override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
            val g2 = g.create() as Graphics2D
            try {
                FlatUIUtils.setRenderingHints(g2)
                g2.translate(x, y)

                var sx = 0
                var sy = 0
                var sw = width
                var sh = height
                val arrowSize = UIScale.scale(ARROW_SIZE)
                when (direction) {
                    SwingConstants.LEFT -> {
                        sx += arrowSize
                        sw -= arrowSize
                    }
                    SwingConstants.TOP -> {
                        sy += arrowSize
                        sh -= arrowSize
                    }
                    SwingConstants.RIGHT -> sw -= arrowSize
                    SwingConstants.BOTTOM -> sh -= arrowSize
                }

                shadowBorder?.paintBorder(c, g2, sx, sy, sw, sh)

                val bx = UIScale.scale(SHADOW_SIZE)
                val by = UIScale.scale(SHADOW_TOP_SIZE)
                val bw = width - UIScale.scale(SHADOW_SIZE + SHADOW_SIZE)
                val bh = height - UIScale.scale(SHADOW_TOP_SIZE + SHADOW_SIZE)
                g2.translate(bx, by)
                val shape = createBalloonShape(bw, bh)

                g2.color = c.background
                g2.fill(shape)

                g2.color = borderColor
                g2.stroke = BasicStroke(UIScale.scale(1f))
                g2.draw(shape)
            } finally {
                g2.dispose()
            }
        }

        private fun createBalloonShape(width: Int, height: Int): Shape {
            val arc = UIScale.scale(ARC).toDouble()
            val xy = UIScale.scale(ARROW_XY).toDouble()
            val awh = UIScale.scale(ARROW_SIZE).toDouble()

            val rect: Shape
            val arrow: Shape
            when (direction) {
                SwingConstants.LEFT -> {
                    rect = RoundRectangle2D.Float(awh.toFloat(), 0f, (width - 1 - awh).toFloat(), (height - 1).toFloat(), arc.toFloat(), arc.toFloat())
                    arrow = FlatUIUtils.createPath(awh, xy, 0.0, xy + awh, awh, xy + awh + awh)
                }
                SwingConstants.TOP -> {
                    rect = RoundRectangle2D.Float(0f, awh.toFloat(), (width - 1).toFloat(), (height - 1 - awh).toFloat(), arc.toFloat(), arc.toFloat())
                    arrow = FlatUIUtils.createPath(xy, awh, xy + awh, 0.0, xy + awh + awh, awh)
                }
                SwingConstants.RIGHT -> {
                    rect = RoundRectangle2D.Float(0f, 0f, (width - 1 - awh).toFloat(), (height - 1).toFloat(), arc.toFloat(), arc.toFloat())
                    val x = width - 1 - awh
                    arrow = FlatUIUtils.createPath(x, xy, x + awh, xy + awh, x, xy + awh + awh)
                }
                SwingConstants.BOTTOM -> {
                    rect = RoundRectangle2D.Float(0f, 0f, (width - 1).toFloat(), (height - 1 - awh).toFloat(), arc.toFloat(), arc.toFloat())
                    val y = height - 1 - awh
                    arrow = FlatUIUtils.createPath(xy, y, xy + awh, y + awh, xy + awh + awh, y)
                }
                else -> throw RuntimeException()
            }

            val area = Area(rect)
            area.add(Area(arrow))
            return area
        }

        companion object {
            private const val ARC = 8
            private const val ARROW_XY = 16
            private const val ARROW_SIZE = 8
            private const val SHADOW_SIZE = 6
            private const val SHADOW_TOP_SIZE = 3
            private const val SHADOW_SIZE2 = SHADOW_SIZE + 2
        }
    }
}