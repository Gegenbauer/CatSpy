package me.gegenbauer.logviewer.databinding.adapter.component

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.logviewer.concurrency.AppScope
import me.gegenbauer.logviewer.concurrency.UI
import me.gegenbauer.logviewer.databinding.adapter.property.EnabledAdapter
import me.gegenbauer.logviewer.databinding.adapter.property.SelectedAdapter
import me.gegenbauer.logviewer.databinding.adapter.property.VisibilityAdapter
import java.awt.event.ItemEvent
import java.awt.event.ItemEvent.SELECTED
import java.awt.event.ItemListener
import javax.swing.JComponent
import javax.swing.JToggleButton

class JToggleButtonAdapter(component: JComponent) : SelectedAdapter, DisposableAdapter,
    EnabledAdapter by JComponentAdapter(component), VisibilityAdapter by JComponentAdapter(component){
    override val selectedChangeListener: ItemListener
        get() = ItemListener { e: ItemEvent ->
            AppScope.launch(Dispatchers.UI) {
                checkedStatusChangeObserver?.invoke(e.stateChange == SELECTED)
            }
        }
    private var checkedStatusChangeObserver: ((Boolean) -> Unit)? = null
    private val cb = component as JToggleButton

    init {
        cb.addItemListener(selectedChangeListener)
    }

    override fun updateSelectedStatus(value: Boolean?) {
        value ?: return
        cb.isSelected = value
    }

    override fun observeSelectedStatusChange(observer: (Boolean?) -> Unit) {
        checkedStatusChangeObserver = observer
    }

    override fun removeSelectedChangeListener() {
        cb.removeItemListener(selectedChangeListener)
    }
}