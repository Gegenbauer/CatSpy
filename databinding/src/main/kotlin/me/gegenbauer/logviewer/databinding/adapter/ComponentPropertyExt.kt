package me.gegenbauer.logviewer.databinding.adapter

import me.gegenbauer.logviewer.databinding.ObservableComponentProperty
import me.gegenbauer.logviewer.databinding.adapter.property.EnabledAdapter
import me.gegenbauer.logviewer.databinding.adapter.property.SelectedAdapter
import me.gegenbauer.logviewer.databinding.adapter.property.TextAdapter
import me.gegenbauer.logviewer.log.GLog
import javax.swing.JComponent

fun enableProperty(component: JComponent) = object : ObservableComponentProperty<Boolean>(component) {
    private val adapter = componentAdapter as? EnabledAdapter

    init {
        adapter ?: kotlin.run { GLog.e(TAG, "[initAdapter] component adapter does not container enabled property") }
        adapter?.observeEnabledStatusChange { newValue ->
            updateValue(newValue)
        }
    }

    override fun setProperty(newValue: Boolean?) {
        super.setProperty(newValue)
        adapter?.updateEnabledStatus(newValue)
    }
}

fun selectedProperty(component: JComponent) = object : ObservableComponentProperty<Boolean>(component) {
    private val adapter = componentAdapter as? SelectedAdapter

    init {
        adapter ?: kotlin.run { GLog.e(TAG, "[initAdapter] component adapter does not container selected property") }
        adapter?.observeSelectedStatus { newValue ->
            updateValue(newValue)
        }
    }

    override fun setProperty(newValue: Boolean?) {
        super.setProperty(newValue)
        adapter?.updateSelectedStatus(newValue)
    }
}

fun textProperty(component: JComponent) = object : ObservableComponentProperty<String>(component) {
    private val adapter = componentAdapter as? TextAdapter

    init {
        adapter ?: kotlin.run { GLog.e(TAG, "[initAdapter] component adapter does not container selected property") }
        adapter?.observeTextChange { newValue ->
            updateValue(newValue)
        }
    }

    override fun setProperty(newValue: String?) {
        super.setProperty(newValue)
        adapter?.updateText(newValue)
    }
}