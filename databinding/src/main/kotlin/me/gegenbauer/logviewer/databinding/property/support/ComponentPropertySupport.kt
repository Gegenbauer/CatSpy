package me.gegenbauer.logviewer.databinding.property.support

import me.gegenbauer.logviewer.databinding.bind.ObservableComponentProperty
import me.gegenbauer.logviewer.databinding.property.adapter.*
import java.awt.event.HierarchyListener
import java.awt.event.ItemListener
import java.beans.PropertyChangeListener
import javax.swing.AbstractButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.event.DocumentListener
import javax.swing.event.ListDataListener
import javax.swing.text.JTextComponent

fun enabledProperty(component: JComponent) = object : ObservableComponentProperty<Boolean>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<Boolean, PropertyChangeListener> = JComponentEnabledProperty(component)


    override fun getDisplayName(): String {
        return "${component.javaClass.simpleName}_${component.hashCode()}_Enabled}"
    }
}

fun visibilityProperty(component: JComponent) = object : ObservableComponentProperty<Boolean>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<Boolean, HierarchyListener> = JComponentVisibilityProperty(component)


    override fun getDisplayName(): String {
        return "${component.javaClass.simpleName}_${component.hashCode()}_Visibility}"
    }
}

fun selectedProperty(component: AbstractButton) = object : ObservableComponentProperty<Boolean>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<Boolean, ItemListener> = AbstractButtonSelectedProperty(component)


    override fun getDisplayName(): String {
        return "${component.javaClass.simpleName}_${component.hashCode()}_Selected}"
    }
}


fun textProperty(component: JTextComponent) = object : ObservableComponentProperty<String>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<String, DocumentListener> = JTextComponentTextProperty(component)


    override fun getDisplayName(): String {
        return "${component.javaClass.simpleName}_${component.hashCode()}_Text}"
    }
}

fun <T> listProperty(component: JComboBox<T>) = object : ObservableComponentProperty<List<T>>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<List<T>, ListDataListener> = JComboBoxListProperty(component)

    override fun getDisplayName(): String {
        return "${component.javaClass.simpleName}_${component.hashCode()}_List}"
    }
}

fun <T> selectedIndexProperty(component: JComboBox<T>) = object : ObservableComponentProperty<Int>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<Int, ItemListener> = JComboBoxSelectedIndexProperty(component)

    override fun getDisplayName(): String {
        return "${component.javaClass.simpleName}_${component.hashCode()}_SelectedIndex}"
    }
}

fun <T> customProperty(component: JComponent, propertyName: String, initValue: T? = null) = object : ObservableComponentProperty<T>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<T, *> = JComponentCustomControllerProperty(component, propertyName)

    init {
        setProperty(initValue)
    }

    override fun getDisplayName(): String {
        return "${component.javaClass.simpleName}_${component.hashCode()}_Custom_$propertyName}"
    }
}


