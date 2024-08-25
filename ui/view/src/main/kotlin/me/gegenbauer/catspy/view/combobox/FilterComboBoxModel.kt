package me.gegenbauer.catspy.view.combobox

import java.io.Serializable
import javax.swing.AbstractListModel
import javax.swing.MutableComboBoxModel

class FilterComboBoxModel<T>(items: List<T> = emptyList()) : AbstractListModel<T>(), MutableComboBoxModel<T>,
    Serializable {
    private val items = mutableListOf<T>()
    private var selectedItem: T? = null

    init {
        this.items.addAll(items)
    }

    /**
     * will be called via reflection
     */
    fun setItems(items: List<T>) {
        this.items.clear()
        this.items.addAll(items.take(MAX_SIZE))
        fireContentsChanged(this, -1, -1)
    }

    fun getItems(): List<T> {
        return items
    }

    override fun getSize(): Int {
        return items.size
    }

    override fun getElementAt(index: Int): T {
        return items[index]
    }

    override fun setSelectedItem(anItem: Any?) {
        if (selectedItem != anItem) {
            selectedItem = anItem as? T
            fireContentsChanged(this, -1, -1)
        }
    }

    override fun getSelectedItem(): Any? {
        return selectedItem
    }

    override fun removeElement(obj: Any?) {
        val index = items.indexOfFirst { it == obj }
        if (index != -1) {
            removeElementAt(index)
        }
    }

    override fun removeElementAt(index: Int) {
        if (getElementAt(index) == selectedItem) {
            if (index == 0) {
                setSelectedItem(getElementAt(1).takeIf { items.size > 1 })
            } else {
                setSelectedItem(getElementAt(index - 1))
            }
        }
        items.removeAt(index)
        fireIntervalRemoved(this, index, index)
    }

    override fun insertElementAt(item: T, index: Int) {
        // no-op
    }

    override fun addElement(item: T) {
        items.add(item)
        fireIntervalAdded(this, items.size - 1, items.size - 1)
        if (items.size == 1 && selectedItem == null && item != null) {
            setSelectedItem(item)
        }
    }

    companion object {
        private const val MAX_SIZE = 40
    }
}