package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.adapter.ComponentAdapter
import me.gegenbauer.logviewer.databinding.adapter.Disposable
import javax.swing.event.ListDataListener

interface ListAdapter<T>: ComponentAdapter {
    val listChangeListener: ListDataListener
    fun updateList(value: List<T>?)
    fun observeListChange(observer: (List<T>?) -> Unit)
    @Disposable
    fun removeListChangeListener()
}

fun <T> List<T>.updateListByLRU(lastUsedItem: T): List<T> {
    return if (lastUsedItem in this) {
        val list = this.toMutableList()
        list.remove(lastUsedItem)
        list.add(0, lastUsedItem)
        list
    } else {
        this
    }
}