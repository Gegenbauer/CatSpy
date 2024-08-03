package me.gegenbauer.catspy.view.combobox

import javax.swing.ComboBoxModel
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent

open class RemoveButtonComboBox<E>(model: ComboBoxModel<E>) : JComboBox<E>(model) {
    private var listener: CellButtonsMouseListener? = null

    constructor(items: Array<E>) : this(object : DefaultComboBoxModel<E>(items) {})

    init {
        setRenderer(PopupListRenderer(this))
    }

    override fun updateUI() {
        listener?.let { listener ->
            getList()?.let {
                it.removeMouseListener(listener)
                it.removeMouseMotionListener(listener)
            }
        }
        super.updateUI()
        getList()?.let { list ->
            listener = CellButtonsMouseListener()
            list.addMouseListener(listener)
            list.addMouseMotionListener(listener)
        }
    }

    private fun getList(): JComponent? {
        return (ui as? RemoveButtonComboBoxUI)?.getList()
    }
}