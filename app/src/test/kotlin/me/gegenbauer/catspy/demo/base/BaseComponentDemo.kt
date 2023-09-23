package me.gegenbauer.catspy.demo.base

import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

abstract class BaseComponentDemo: JPanel(), ComponentDemo {

    init {
        layout = BorderLayout()
    }

    override val component: JComponent
        get() = this
}