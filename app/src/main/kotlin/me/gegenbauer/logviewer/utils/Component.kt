package me.gegenbauer.logviewer.utils

import javax.swing.JComponent

infix fun <T: JComponent> T.applyTooltip(tooltip: String?): T {
    this.toolTipText = tooltip
    return this
}