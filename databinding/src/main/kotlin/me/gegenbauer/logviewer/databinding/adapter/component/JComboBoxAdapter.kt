package me.gegenbauer.logviewer.databinding.adapter.component

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.logviewer.concurrency.UI
import me.gegenbauer.logviewer.concurrency.ViewModelScope
import me.gegenbauer.logviewer.databinding.adapter.property.*
import me.gegenbauer.logviewer.databinding.withAllListenerDisabled
import java.awt.event.ItemListener
import java.beans.PropertyChangeListener
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

class JComboBoxAdapter<T>(component: JComponent) : EditableAdapter, ListAdapter<T>, SelectedIndexAdapter,
    EnabledAdapter by JComponentAdapter(component), VisibilityAdapter by JComponentAdapter(component) {

    private val comboBox = component as JComboBox<T>
    private val scope = ViewModelScope()

    init {
        comboBox.addPropertyChangeListener(PROPERTY_EDITABLE, editableChangeListener)
        comboBox.model.addListDataListener(listChangeListener)
        comboBox.addItemListener(selectedIndexChangeListener)
    }

    override val editableChangeListener: PropertyChangeListener
        get() = PropertyChangeListener { evt ->
            editableChangeObserver?.invoke(evt.newValue as Boolean)
        }
    private var editableChangeObserver: ((Boolean) -> Unit)? = null

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


    private var listChangeObserver: ((List<T>) -> Unit)? = null

    private fun JComboBox<T>.getAllItems(): List<T> {
        return mutableListOf<T>().apply {
            for (i in 0 until itemCount) {
                add(getItemAt(i))
            }
        }
    }

    override val listChangeListener: ListDataListener
        get() = object : ListDataListener {
            private var contentChangeJob: Job? = null

            override fun intervalAdded(e: ListDataEvent) {
                // do nothing
            }

            override fun intervalRemoved(e: ListDataEvent) {
                // do nothing
            }

            override fun contentsChanged(e: ListDataEvent) {
                contentChangeJob?.cancel()
                contentChangeJob = scope.launch(Dispatchers.UI) {
                    delay(10)
                    comboBox.getAllItems().let { listChangeObserver?.invoke(it) }
                }
            }
        }

    override fun removeListChangeListener() {
        comboBox.model.removeListDataListener(listChangeListener)
    }

    override fun observeListChange(observer: (List<T>?) -> Unit) {
        listChangeObserver = observer
    }

    override fun updateList(value: List<T>?) {
        value ?: return
        comboBox.withAllListenerDisabled {
            removeAllItems()
            value.forEach(comboBox::addItem)
        }
        comboBox.editor.item = value.firstOrNull()
        listChangeObserver?.invoke(value)
    }

    private var selectedIndexChangeObserver: ((Int?) -> Unit)? = null
    override val selectedIndexChangeListener: ItemListener
        get() = ItemListener {
            selectedIndexChangeObserver?.invoke(comboBox.getAllItems().indexOf(it.item))
        }

    override fun updateSelectedIndex(value: Int?) {
        value ?: return
        if (value >= 0) {
            comboBox.selectedIndex = value
        }
    }

    override fun observeSelectedIndexChange(observer: (Int?) -> Unit) {
        selectedIndexChangeObserver = observer
    }

    override fun removeSelectedIndexChangeListener() {
        comboBox.removeItemListener(selectedIndexChangeListener)
    }

    companion object {
        private const val PROPERTY_EDITABLE = "editable"
    }
}