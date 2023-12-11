package me.gegenbauer.catspy.configuration

import javax.swing.JComponent

interface ISettingsGroup {
    val title: String

    val subGroups: List<ISettingsGroup>
        get() = emptyList()

    fun buildComponent(): JComponent
}