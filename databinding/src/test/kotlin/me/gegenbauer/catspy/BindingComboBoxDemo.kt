package me.gegenbauer.catspy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.databinding.bind.Bindings
import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty
import me.gegenbauer.catspy.databinding.bind.updateListByLRU
import me.gegenbauer.catspy.databinding.property.support.listProperty
import me.gegenbauer.catspy.databinding.property.support.selectedIndexProperty
import me.gegenbauer.catspy.databinding.property.support.textProperty
import me.gegenbauer.catspy.log.GLog
import me.gegenbauer.catspy.ui.button.GButton
import me.gegenbauer.catspy.ui.combobox.darkComboBox
import me.gegenbauer.catspy.utils.editorComponent
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JPanel

fun main() {
    AppScope.launch(Dispatchers.UI) {
        val frame = JFrame()
        GLog.DEBUG = true
        frame.size = Dimension(500, 500)
        val panel = JPanel()
        val bt = GButton("C")
        panel.add(bt)
        val cb = darkComboBox()
        cb.preferredSize = Dimension(200, 30)
        panel.add(cb)
        cb.isEditable = true
        val vm = ComboBoxViewModel()
        bt.addActionListener {
            vm.items.updateValue(listOf("A", "B", "C"))
        }
        Bindings.bind(listProperty(cb), vm.items)
        Bindings.bind(selectedIndexProperty(cb), vm.selectedIndex)
        Bindings.bind(textProperty(cb.editorComponent), vm.currentContent)
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