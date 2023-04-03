package me.gegenbauer.logviewer.databinding.adapter.component

import me.gegenbauer.logviewer.databinding.adapter.property.SelectedAdapter
import java.awt.event.ItemEvent
import java.awt.event.ItemEvent.SELECTED
import java.awt.event.ItemListener
import javax.swing.JComponent
import javax.swing.JToggleButton

class JToggleButtonAdapter(component: JComponent) : SelectedAdapter, DisposableAdapter {
    private val itemListener = ItemListener { e: ItemEvent ->
        checkedStatusChangeObserver?.invoke(e.stateChange == SELECTED)
    }
    private var checkedStatusChangeObserver: ((Boolean) -> Unit)? = null
    private val cb = component as JToggleButton

    init {
        cb.addItemListener(itemListener)
    }

    override fun updateSelectedStatus(value: Boolean?) {
        value ?: return
        cb.isSelected = value
    }

    override fun observeSelectedStatus(observer: (Boolean?) -> Unit) {
        checkedStatusChangeObserver = observer
    }

    override fun removeSelectedChangeListener() {
        cb.removeItemListener(itemListener)
    }
}