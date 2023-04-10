package me.gegenbauer.logviewer.databinding.adapter

import me.gegenbauer.logviewer.databinding.adapter.component.JComboBoxAdapter
import me.gegenbauer.logviewer.databinding.adapter.component.JComponentAdapter
import me.gegenbauer.logviewer.databinding.adapter.component.JTextComponentAdapter
import me.gegenbauer.logviewer.databinding.adapter.component.JToggleButtonAdapter
import me.gegenbauer.logviewer.log.GLog
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JToggleButton
import javax.swing.text.JTextComponent

object ComponentAdapterFactory {
    private const val TAG = "ComponentAdapterFactory"
    private val adapters = mutableMapOf<Class<out JComponent>, Class<out ComponentAdapter>>()

    init {
        adapters.putIfAbsent(JToggleButton::class.java, JToggleButtonAdapter::class.java)
        adapters.putIfAbsent(JTextComponent::class.java, JTextComponentAdapter::class.java)
        adapters.putIfAbsent(JComboBox::class.java, JComboBoxAdapter::class.java)
        adapters.putIfAbsent(JComponent::class.java, JComponentAdapter::class.java)
    }

    fun getComponentAdapter(component: JComponent): ComponentAdapter? {
        val adapterClazz = adapters[component.javaClass]
        if (adapterClazz == null) {
            val adapterClazzKeyFromSupper = findFromSupperClazz(component) { adapters.containsKey(it) }
            if (adapterClazzKeyFromSupper == null) {
                GLog.d(TAG, "[getComponentAdapter] no adapter found for component: ${component.javaClass}")
                return null
            }
            val adapterClazzFromSupper = adapters[adapterClazzKeyFromSupper]
            return adapterClazzFromSupper?.getDeclaredConstructor(JComponent::class.java)?.newInstance(component)
        }
        return adapterClazz.getDeclaredConstructor(JComponent::class.java).newInstance(component)
    }

    private fun findFromSupperClazz(component: JComponent, predicate: (Class<out JComponent>) -> Boolean): Class<out JComponent>? {
        var clazz: Class<out JComponent>? = component.javaClass
        while (clazz != null && JComponent::class.java.isAssignableFrom(clazz)) {
            if (predicate(clazz)) {
                return clazz
            }
            clazz = clazz.superclass as? Class<out JComponent>
        }
        return null
    }
}