package me.gegenbauer.catspy.utils.ui

import java.awt.Component
import java.awt.Toolkit

fun setSizePercentage(component: Component, percentageHorizontal: Int, percentageVertical: Int = percentageHorizontal) {
    val screenSize = Toolkit.getDefaultToolkit().screenSize
    val width = screenSize.width * percentageHorizontal / 100
    val height = screenSize.height * percentageVertical / 100
    component.setSize(width, height)
    component.preferredSize = component.size
}