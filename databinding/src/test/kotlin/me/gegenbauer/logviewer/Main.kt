package me.gegenbauer.logviewer

import me.gegenbauer.logviewer.databinding.BindType
import me.gegenbauer.logviewer.databinding.Bindings
import me.gegenbauer.logviewer.databinding.ObservableViewModelProperty
import me.gegenbauer.logviewer.databinding.adapter.enableProperty
import me.gegenbauer.logviewer.databinding.adapter.selectedProperty
import me.gegenbauer.logviewer.databinding.adapter.textProperty
import me.gegenbauer.logviewer.log.GLog
import java.awt.Dimension
import javax.swing.*

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame()
        GLog.DEBUG = true
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

        val vm = ViewModel()

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
        Bindings.bind(enableProperty(textField1), vm.checked)
        Bindings.bind(enableProperty(textField1), vm.checked)
        Bindings.bind(textProperty(textField1), vm.text, BindType.ONE_WAY_TO_SOURCE)
        Bindings.bind(textProperty(textField2), vm.text, BindType.ONE_WAY_TO_TARGET)
        frame.isVisible = true
        frame.addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(e: java.awt.event.WindowEvent?) {
                // TODO 实现绑定接触，并检测是否成功解除
            }
        })
    }
}


// MVVM 示例
// ViewModel
class ViewModel {
    val checked = ObservableViewModelProperty(false)
    val text = ObservableViewModelProperty("asdasdasd")
}