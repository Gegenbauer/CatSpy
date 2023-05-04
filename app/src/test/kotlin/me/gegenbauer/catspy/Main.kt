package me.gegenbauer.catspy

import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.Theme
import com.github.weisj.darklaf.ui.button.DarkButtonBorder
import com.github.weisj.darklaf.ui.button.DarkButtonUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.APP_LAUNCH
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.configuration.UIConfManager
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.ui.button.GButton
import me.gegenbauer.catspy.ui.combobox.filterComboBox
import me.gegenbauer.catspy.viewmodel.GlobalViewModel
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.plaf.UIResource

fun main() {
    AppScope.launch(Dispatchers.UI) {
        withContext(Dispatchers.APP_LAUNCH) {
            GLog.debug = UIConfManager.uiConf.debug
            ThemeManager.init()
            GlobalViewModel.init()
        }
        ThemeManager.installTheme()
        LafManager.registerInitTask { theme: Theme, defaults: UIDefaults ->
            defaults[DarkButtonUI.KEY_VARIANT] = DarkButtonUI.VARIANT_BORDERLESS
        }
        val frame = JFrame()
        frame.size = Dimension(500, 500)
        val panel = JPanel()
        val button1 = CustomButton("Button1")
        val button2 = CustomButton("Button2")
        val progressBar = JProgressBar(JProgressBar.HORIZONTAL, 0, 100)
        progressBar.value = 50
        button1.isContentAreaFilled = true
        button1.isRolloverEnabled = true
        val comboBox = filterComboBox()
        comboBox.addItem("1")
        val comboBox2 = JComboBox(arrayOf("1", "2", "3"))
        comboBox2.isEditable = true
        comboBox2.preferredSize = Dimension(100, 30)
        comboBox.editor.editorComponent.addKeyListener(object : KeyAdapter() {
            override fun keyReleased(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    println("Enter")
                }
            }
        })
        panel.add(button1)
        panel.add(button2)
        panel.add(comboBox)
        panel.add(comboBox2)
        panel.add(progressBar)
        frame.add(panel)
        //ThemeManager.applyTempTheme()
        frame.isVisible = true
    }
}

class CustomButton(text: String): GButton(text)

class CustomUI: DarkButtonUI() {
    override fun paintButtonBackground(g: Graphics?, c: JComponent) {
        val g2 = g as Graphics2D?
        val b = c as AbstractButton
        if (shouldDrawBackground(b)) {
            val arcSize = getArc(c)
            val width = c.getWidth()
            val height = c.getHeight()
            var margin = b.margin
            if (margin is UIResource) {
                margin = null
            }

            if ((c as JButton).model.isRollover) {
              //  paintBorderlessBackground(b, g2, arcSize, width, height, margin)
            } else {
                if (b.border is DarkButtonBorder) {
                    paintDarklafBorderBackground(b, g2, arcSize, width, height)
                } else {
                    paintDefaultBackground(b, g2, width, height)
                }
            }
        }
    }
}