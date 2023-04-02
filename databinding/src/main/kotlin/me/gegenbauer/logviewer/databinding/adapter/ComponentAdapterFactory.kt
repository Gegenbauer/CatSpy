package me.gegenbauer.logviewer.databinding.adapter

import me.gegenbauer.logviewer.databinding.adapter.component.JCheckBoxAdapter
import me.gegenbauer.logviewer.databinding.adapter.component.JTextFieldAdapter
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JTextField

object ComponentAdapterFactory {
    private val adapters = mutableMapOf<Class<out JComponent>, Class<out ComponentAdapter>>()

    init {
        adapters.putIfAbsent(JCheckBox::class.java, JCheckBoxAdapter::class.java)
        adapters.putIfAbsent(JTextField::class.java, JTextFieldAdapter::class.java)
    }

    fun getComponentAdapter(component: JComponent): ComponentAdapter {
        val adapterClazz = adapters[component.javaClass] ?: throw IllegalArgumentException("No adapter found for ${component.javaClass}")
        return adapterClazz.getDeclaredConstructor(JComponent::class.java).newInstance(component)
    }
}