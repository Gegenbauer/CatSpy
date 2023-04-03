package me.gegenbauer.logviewer.databinding.adapter.component

import me.gegenbauer.logviewer.databinding.adapter.property.EditableAdapter
import java.beans.PropertyChangeListener
import javax.swing.JComboBox
import javax.swing.JComponent

class JComboBoxAdapter<T>(component: JComponent): EditableAdapter {
    private val comboBox = component as JComboBox<T>
    override val editableChangeListener: PropertyChangeListener
        get() = PropertyChangeListener { evt ->
            editableChangeObserver?.invoke(evt.newValue as Boolean)
        }
    private var editableChangeObserver: ((Boolean) -> Unit)? = null

    init {
        component.addPropertyChangeListener(PROPERTY_EDITABLE, editableChangeListener)
    }

    override fun updateEditableStatus(value: Boolean?) {
        value ?: return
        comboBox.isEditable = value
    }

    override fun observeEditableStatusChange(observer: (Boolean?) -> Unit) {
        editableChangeObserver = observer
    }

    override fun removeEditableChangeListener() {
        comboBox.removePropertyChangeListener(PROPERTY_EDITABLE, editableChangeListener)
    }

    companion object {
        private const val PROPERTY_EDITABLE = "editable"
    }
}