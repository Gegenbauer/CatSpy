package me.gegenbauer.catspy.configuration

import javax.swing.JComponent

interface ISettingsGroup {
    val title: String

    val subGroups: List<ISettingsGroup>
        get() = emptyList()

    fun initGroup() {}

    fun addRow(label: String, comp: JComponent): JComponent

    fun addRow(label: String, tooltip: String?, comp: JComponent): JComponent

    fun end()

    fun buildComponent(): JComponent
}