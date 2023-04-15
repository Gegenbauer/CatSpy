package me.gegenbauer.logviewer.databinding.adapter.property

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.logviewer.concurrency.UI
import me.gegenbauer.logviewer.concurrency.ViewModelScope
import me.gegenbauer.logviewer.databinding.withAllListenerDisabled
import javax.swing.JComboBox
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

class JComboBoxListProperty<ITEM>(component: JComboBox<ITEM>) :
    BasePropertyAdapter<JComboBox<ITEM>, List<ITEM>, ListDataListener>(component) {
    private val scope = ViewModelScope()

    init {
        component.model.addListDataListener(propertyChangeListener)
    }

    override val propertyChangeListener: ListDataListener
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
                    component.getAllItems().let { propertyChangeObserver?.invoke(it) }
                }
            }
        }

    private fun JComboBox<ITEM>.getAllItems(): List<ITEM> {
        return mutableListOf<ITEM>().apply {
            for (i in 0 until itemCount) {
                add(getItemAt(i))
            }
        }
    }

    override fun removePropertyChangeListener() {
        component.model.removeListDataListener(propertyChangeListener)
    }

    override fun updateValue(value: List<ITEM>?) {
        value ?: return
        component.withAllListenerDisabled {
            removeAllItems()
            value.forEach(component::addItem)
        }
        component.editor.item = value.firstOrNull()
        propertyChangeObserver?.invoke(value)
    }

}