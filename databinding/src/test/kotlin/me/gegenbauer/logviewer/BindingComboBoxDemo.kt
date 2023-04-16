package me.gegenbauer.logviewer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.logviewer.concurrency.AppScope
import me.gegenbauer.logviewer.concurrency.UI
import me.gegenbauer.logviewer.databinding.bind.Bindings
import me.gegenbauer.logviewer.databinding.bind.ObservableViewModelProperty
import me.gegenbauer.logviewer.databinding.bind.updateListByLRU
import me.gegenbauer.logviewer.databinding.property.support.listProperty
import me.gegenbauer.logviewer.databinding.property.support.selectedIndexProperty
import me.gegenbauer.logviewer.databinding.property.support.textProperty
import me.gegenbauer.logviewer.log.GLog
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.text.JTextComponent

fun main() {
    AppScope.launch(Dispatchers.UI) {
        val frame = JFrame()
        GLog.DEBUG = true
        frame.size = Dimension(500, 500)
        val panel = JPanel()
        val bt = JButton("C")
        panel.add(bt)
        val cb = JComboBox<String>()
        cb.preferredSize = Dimension(200, 30)
        panel.add(cb)
        cb.isEditable = true
        val vm = ComboBoxViewModel()
        bt.addActionListener {
            vm.items.updateValue(listOf("A", "B", "C"))
        }
        Bindings.bind(listProperty(cb), vm.items)
        Bindings.bind(selectedIndexProperty(cb), vm.selectedIndex)
        Bindings.bind(textProperty(cb.editor.editorComponent as JTextComponent), vm.currentContent)
        vm.selectedIndex.addObserver {
            it ?: return@addObserver
            val currentItems = vm.items.value
            currentItems ?: return@addObserver
            vm.items.updateValue(currentItems.updateListByLRU(currentItems[it]))
        }
        frame.add(panel)
        frame.isVisible = true
    }
}

class ComboBoxViewModel {
    val items = ObservableViewModelProperty<List<String>>()
    val selectedIndex = ObservableViewModelProperty<Int>()
    val currentContent = ObservableViewModelProperty<String>()
}