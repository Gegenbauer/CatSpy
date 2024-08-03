package me.gegenbauer.catspy.log.binding

import me.gegenbauer.catspy.configuration.Rotation
import me.gegenbauer.catspy.configuration.currentSettings
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.property.support.listProperty
import me.gegenbauer.catspy.databinding.property.support.selectedItemProperty
import me.gegenbauer.catspy.databinding.property.support.textProperty
import me.gegenbauer.catspy.java.ext.getEnum
import me.gegenbauer.catspy.utils.ui.editorComponent
import javax.swing.JComboBox

class LogMainBinding : ContextService {
    private val logSettings = currentSettings.logSettings

    val adbServerStatusWarningVisibility = ObservableValueProperty(false)
    val connectedDevices = ObservableValueProperty(arrayListOf<String>().toList())
    val currentDevice = ObservableValueProperty<String>()

    val pauseAll = ObservableValueProperty(false)

    val rotation = ObservableValueProperty(getEnum<Rotation>(logSettings.rotation))

    val splitPanelDividerLocation = ObservableValueProperty(logSettings.dividerLocation)

    fun bindNormalCombo(
        comboBox: JComboBox<String>,
        listProperty: ObservableValueProperty<List<String>>,
        editorContentProperty: ObservableValueProperty<String>,
    ) {
        listProperty(comboBox) bindDual listProperty
        selectedItemProperty(comboBox) bindDual editorContentProperty
        textProperty(comboBox.editorComponent) bindDual editorContentProperty
    }
}