package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.adapter.ComponentAdapter
import me.gegenbauer.logviewer.databinding.adapter.Disposable
import java.awt.event.ItemListener

interface SelectedIndexAdapter: ComponentAdapter {
    val selectedIndexChangeListener: ItemListener

    fun updateSelectedIndex(value: Int?)

    fun observeSelectedIndexChange(observer: (Int?) -> Unit)

    @Disposable
    fun removeSelectedIndexChangeListener()
}