package me.gegenbauer.catspy.view.combobox

import javax.swing.DefaultComboBoxModel

open class HistoryComboBoxModel<T>(items: List<T> = emptyList()) : DefaultComboBoxModel<HistoryItem<T>>() {

    init {
        items.toHistoryItemList().forEach { super.addElement(it) }
    }

    override fun addElement(anObject: HistoryItem<T>) {
        insertElement(anObject)
    }

    @Suppress("UNCHECKED_CAST")
    private fun insertElement(anObject: HistoryItem<T>) {
        val selectedItem = selectedItem as HistoryItem<T>?
        if (size == 0) {
            super.addElement(anObject)
            return
        }
        if (containsItem(anObject)) {
            return
        }
        if (size >= MAX_SIZE) {
            removeElementAt(size - 1)
        }
        super.insertElementAt(anObject, findInsertIndex(anObject))
        if (selectedItem != null) {
            setSelectedItem(selectedItem)
        }
        if (selectedItem == null) {
            setSelectedItem(getElementAt(0))
        }
    }

    /**
     * default sort in descending order
     */
    private fun findInsertIndex(item: HistoryItem<T>): Int {
        if (size == 0) {
            return 0
        }
        for (index in 0 until size) {
            if (getElementAt(index) < item) {
                return index
            }
        }
        return size
    }

    private fun containsItem(item: HistoryItem<T>): Boolean {
        for (index in 0 until size) {
            if (getElementAt(index) == item) {
                return true
            }
        }
        return false
    }

    companion object {
        private const val MAX_SIZE = 20
    }
}