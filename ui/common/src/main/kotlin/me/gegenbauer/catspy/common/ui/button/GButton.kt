package me.gegenbauer.catspy.common.ui.button

import javax.swing.Icon
import javax.swing.JButton

open class GButton(text: String? = null, icon: Icon? = null): JButton(text, icon) {

    constructor(icon: Icon?): this(null, icon)

    constructor(icon: Icon? = null, tooltip: String? = null): this(null, icon) {
        toolTipText = tooltip
    }

    init {
        isRolloverEnabled = true
    }
}