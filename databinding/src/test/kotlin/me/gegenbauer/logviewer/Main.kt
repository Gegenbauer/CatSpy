package me.gegenbauer.logviewer

import me.gegenbauer.logviewer.databinding.Bindings
import me.gegenbauer.logviewer.databinding.ComponentProperty
import me.gegenbauer.logviewer.databinding.ObservableProperty
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
        val textField1 = JTextField()
        textField1.isEditable = true
        textField1.preferredSize = Dimension(200,30)
        val textField2 = JTextField()
        textField2.preferredSize = Dimension(200,30)
        panel.add(cb1)
        panel.add(cb2)
        panel.add(textField1)
        panel.add(textField2)
        frame.add(panel)

        val vm = ViewModel()
        Bindings.bind(cb1, ComponentProperty.Selected, vm.checked)
        Bindings.bind(cb2, ComponentProperty.Selected, vm.checked)
        Bindings.bind(textField1, ComponentProperty.Text, vm.text)
        Bindings.bind(textField2, ComponentProperty.Text, vm.text)
        Bindings.bind(textField2, ComponentProperty.Enabled, vm.checked)
        frame.isVisible = true
    }
}


// MVVM 示例
// ViewModel
class ViewModel {
    val checked = ObservableProperty(false)
    val text = ObservableProperty("asdasdasd")
}