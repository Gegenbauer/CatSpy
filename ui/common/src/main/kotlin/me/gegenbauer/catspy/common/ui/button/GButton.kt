package me.gegenbauer.catspy.common.ui.button

import javax.swing.Icon
import javax.swing.JButton

open class GButton(text: String? = null, icon: Icon? = null): JButton(text, icon) {

    constructor(icon: Icon?): this(null, icon)

    init {
        isRolloverEnabled = true
    }
}