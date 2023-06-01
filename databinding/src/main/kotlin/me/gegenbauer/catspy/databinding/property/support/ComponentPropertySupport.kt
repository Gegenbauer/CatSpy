package me.gegenbauer.catspy.databinding.property.support

import me.gegenbauer.catspy.databinding.bind.ObservableComponentProperty
import me.gegenbauer.catspy.databinding.property.adapter.*
import java.awt.event.HierarchyListener
import java.awt.event.ItemListener
import java.beans.PropertyChangeListener
import javax.swing.AbstractButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JSplitPane
import javax.swing.event.DocumentListener
import javax.swing.event.ListDataListener
import javax.swing.text.JTextComponent

fun enabledProperty(component: JComponent): ObservableComponentProperty<Boolean> = object : ObservableComponentProperty<Boolean>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<Boolean, PropertyChangeListener> = JComponentEnabledProperty(component)

    override fun createProperty(component: JComponent) = enabledProperty(component)

    override fun getValueType(): String {
        return "Enabled"
    }
}

fun visibilityProperty(component: JComponent): ObservableComponentProperty<Boolean> = object : ObservableComponentProperty<Boolean>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<Boolean, HierarchyListener> = JComponentVisibilityProperty(component)

    override fun createProperty(component: JComponent) = visibilityProperty(component)

    override fun getValueType(): String {
        return "Visibility"
    }
}

fun selectedProperty(component: AbstractButton): ObservableComponentProperty<Boolean> = object : ObservableComponentProperty<Boolean>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<Boolean, ItemListener> = AbstractButtonSelectedProperty(component)

    override fun createProperty(component: JComponent) = selectedProperty(component as AbstractButton)

    override fun getValueType(): String {
        return "Selected"
    }
}

fun textProperty(component: JTextComponent): ObservableComponentProperty<String> = object : ObservableComponentProperty<String>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<String, DocumentListener> = JTextComponentTextProperty(component)

    override fun createProperty(component: JComponent) = textProperty(component as JTextComponent)

    override fun getValueType(): String {
        return "Text"
    }
}

fun textProperty(component: JLabel): ObservableComponentProperty<String> = object : ObservableComponentProperty<String>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<String, PropertyChangeListener> = JLabelTextProperty(component)

    override fun createProperty(component: JComponent) = textProperty(component as JLabel)

    override fun getValueType(): String {
        return "Text"
    }
}

fun <T> listProperty(component: JComboBox<T>): ObservableComponentProperty<List<T>> = object : ObservableComponentProperty<List<T>>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<List<T>, ListDataListener> = JComboBoxListProperty(component)

    @Suppress("UNCHECKED_CAST")
    override fun createProperty(component: JComponent) = listProperty(component as JComboBox<T>)

    override fun getValueType(): String {
        return "List"
    }
}

fun <T> selectedIndexProperty(component: JComboBox<T>): ObservableComponentProperty<Int> = object : ObservableComponentProperty<Int>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<Int, ItemListener> = JComboBoxSelectedIndexProperty(component)

    @Suppress("UNCHECKED_CAST")
    override fun createProperty(component: JComponent) = selectedIndexProperty(component as JComboBox<T>)

    override fun getValueType(): String {
        return "SelectedIndex"
    }
}

fun <T> customProperty(component: JComponent, propertyName: String, initValue: T? = null): ObservableComponentProperty<T> = object : ObservableComponentProperty<T>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<T, *> = JComponentCustomControllerProperty(component, propertyName)

    init {
        setProperty(initValue)
    }

    @Suppress("UNCHECKED_CAST")
    override fun createProperty(component: JComponent) = customProperty(component as JComboBox<T>, propertyName, initValue)

    override fun getValueType(): String {
        return "Custom_$propertyName"
    }
}

fun dividerProperty(component: JSplitPane): ObservableComponentProperty<Int> = object : ObservableComponentProperty<Int>(component) {
    override fun getPropertyAdapterImpl(): PropertyAdapter<Int, PropertyChangeListener> = JSplitPaneDividerProperty(component)

    override fun createProperty(component: JComponent) = dividerProperty(component as JSplitPane)

    override fun getValueType(): String {
        return "Divider"
    }
}

