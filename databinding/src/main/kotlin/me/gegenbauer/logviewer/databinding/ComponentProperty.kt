package me.gegenbauer.logviewer.databinding

import java.awt.Component
import java.awt.event.ItemEvent
import java.awt.event.ItemEvent.SELECTED
import javax.swing.AbstractButton
import javax.swing.JLabel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

interface ObservablePropertyFetcher {
    fun <T> getObservableProperty(component: Component): ObservableProperty<T>
}

enum class ComponentProperty : ObservablePropertyFetcher {
    Selected {
        override fun <T> getObservableProperty(
            component: Component
        ): ObservableProperty<T> {
            return SelectedProperty(component) as ObservableProperty<T>
        }
    },
    Enabled {
        override fun <T> getObservableProperty(
            component: Component
        ): ObservableProperty<T> {
            return EnabledProperty(component) as ObservableProperty<T>
        }
    },
    Text {
        override fun <T> getObservableProperty(
            component: Component
        ): ObservableProperty<T> {
            return TextProperty(component) as ObservableProperty<T>
        }
    }
}

open class BaseProperty<T>(protected val component: Component) : ObservableProperty<T>()

class SelectedProperty(component: Component) : BaseProperty<Boolean>(component) {
    private val itemListener = { e: ItemEvent ->
        setValue(e.stateChange == SELECTED)
    }

    init {
        when (component) {
            is AbstractButton -> component.addItemListener(itemListener)
        }
    }

    override fun onPropertyChange(source: ObservableProperty<*>, newValue: Boolean?, oldValue: Boolean?) {
        super.onPropertyChange(source, newValue, oldValue)
        when (component) {
            is AbstractButton -> component.isSelected = newValue ?: false
        }
    }
}

class EnabledProperty(component: Component) : BaseProperty<Boolean>(component) {

    init {
        if (component is AbstractButton) {
            component.addChangeListener {
                val button = it.source as AbstractButton
                setValue(button.isEnabled)
            }
        }
    }

    override fun onPropertyChange(source: ObservableProperty<*>, newValue: Boolean?, oldValue: Boolean?) {
        super.onPropertyChange(source, newValue, oldValue)
        if (component is AbstractButton) {
            component.isEnabled = newValue ?: false
        }
    }
}

class TextProperty(component: Component) : BaseProperty<String>(component) {
    init {
        if (component is AbstractButton) {
            component.addChangeListener {
                val button = it.source as AbstractButton
                setValue(button.text)
            }
        } else if (component is JTextField) {
            component.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) {
                    setValue(component.text)
                }

                override fun removeUpdate(e: DocumentEvent) {
                    setValue(component.text)
                }

                override fun changedUpdate(e: DocumentEvent) {
                    setValue(component.text)
                }
            })
        } else if (component is JLabel) {
            component.addPropertyChangeListener("text") {
                val label = it.source as JLabel
                setValue(label.text)
            }
        }
    }

    override fun onPropertyChange(source: ObservableProperty<*>, newValue: String?, oldValue: String?) {
        super.onPropertyChange(source, newValue, oldValue)
        if (newValue == oldValue) {
            return
        }
        if (component is AbstractButton) {
            component.text = getValue()
        } else if (component is JTextField) {
            component.text = getValue()
        } else if (component is JLabel) {
            component.text = getValue()
        }
    }
}