package me.gegenbauer.logviewer.databinding.adapter

import me.gegenbauer.logviewer.databinding.ObservableComponentProperty
import me.gegenbauer.logviewer.databinding.adapter.property.*
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

    override fun getDisplayName(): String {
        return "${component.javaClass.simpleName}_${component.hashCode()}_Enabled}"
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
        adapter?.observeSelectedStatusChange { newValue ->
            updateValue(newValue)
        }
    }

    override fun getDisplayName(): String {
        return "${component.javaClass.simpleName}_${component.hashCode()}_Selected}"
    }

    override fun setProperty(newValue: Boolean?) {
        super.setProperty(newValue)
        adapter?.updateSelectedStatus(newValue)
    }
}

fun textProperty(component: JComponent) = object : ObservableComponentProperty<String>(component) {
    private val adapter = componentAdapter as? TextAdapter

    init {
        adapter ?: kotlin.run { GLog.e(TAG, "[initAdapter] component adapter does not container text property") }
        adapter?.observeTextChange { newValue ->
            updateValue(newValue)
        }
    }

    override fun getDisplayName(): String {
        return "${component.javaClass.simpleName}_${component.hashCode()}_Text}"
    }

    override fun setProperty(newValue: String?) {
        super.setProperty(newValue)
        adapter?.updateText(newValue)
    }
}

fun <T> listProperty(component: JComponent) = object : ObservableComponentProperty<List<T>>(component) {
    private val adapter = componentAdapter as? ListAdapter<T>

    init {
        adapter ?: kotlin.run { GLog.e(TAG, "[initAdapter] component adapter does not container list property") }
        adapter?.observeListChange { newValue ->
            updateValue(newValue)
        }
    }

    override fun getDisplayName(): String {
        return "${component.javaClass.simpleName}_${component.hashCode()}_List}"
    }

    override fun setProperty(newValue: List<T>?) {
        super.setProperty(newValue)
        adapter?.updateList(newValue)
    }
}

fun selectedIndexProperty(component: JComponent) = object : ObservableComponentProperty<Int>(component) {
    private val adapter = componentAdapter as? SelectedIndexAdapter

    init {
        adapter ?: kotlin.run { GLog.e(TAG, "[initAdapter] component adapter does not container selected index property") }
        adapter?.observeSelectedIndexChange { newValue ->
            updateValue(newValue)
        }
    }

    override fun getDisplayName(): String {
        return "${component.javaClass.simpleName}_${component.hashCode()}_SelectedIndex}"
    }

    override fun setProperty(newValue: Int?) {
        super.setProperty(newValue)
        adapter?.updateSelectedIndex(newValue)
    }
}