package me.gegenbauer.catspy.databinding

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.CPU
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.databinding.bind.Bindings
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.databinding.property.support.enabledProperty
import me.gegenbauer.catspy.databinding.property.support.selectedProperty
import me.gegenbauer.catspy.databinding.property.support.textProperty
import me.gegenbauer.catspy.glog.GLog
import java.awt.Dimension
import javax.swing.*

fun main() {
    AppScope.launch(Dispatchers.UI) {
        val frame = JFrame()
        GLog.debug = true
        frame.size = Dimension(500, 500)
        val panel = JPanel()
        val cb1 = JCheckBox("1")
        val cb2 = JCheckBox("2")
        val bt = JButton("remove/add")
        bt.preferredSize = Dimension(50,30)
        val textField1 = JTextField()
        textField1.isEditable = true
        textField1.preferredSize = Dimension(200, 30)
        val textField2 = JTextField()
        textField2.preferredSize = Dimension(200, 30)
        panel.add(cb1)
        panel.add(cb2)
        panel.add(bt)
        panel.add(textField1)
        panel.add(textField2)
        panel.add(textField2)
        frame.add(panel)

        val vm = Binding()

        bt.addActionListener {
            if (panel.components.contains(cb1)) {
                panel.remove(textField2)
                panel.invalidate()
            } else {
                panel.add(textField2)
                panel.invalidate()
            }
        }


        Bindings.bind(selectedProperty(cb1), vm.checked)
        Bindings.bind(selectedProperty(cb2), vm.checked)
        Bindings.bind(enabledProperty(textField1), vm.checked)
        Bindings.bind(enabledProperty(textField1), vm.checked)
        Bindings.bind(textProperty(textField1), vm.text)
        Bindings.bind(textProperty(textField2), vm.text)
        frame.isVisible = true
        frame.addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(e: java.awt.event.WindowEvent?) {
                // TODO 实现绑定接触，并检测是否成功解除
            }
        })
        repeat(100) {
            withContext(Dispatchers.CPU) {
                delay(2)
                vm.text.updateValue(System.currentTimeMillis().toString())
            }
        }
    }
}


// MVVM 示例
// ViewModel
class Binding {
    val checked = ObservableValueProperty(false)
    val text = ObservableValueProperty("asdasdasd")
}