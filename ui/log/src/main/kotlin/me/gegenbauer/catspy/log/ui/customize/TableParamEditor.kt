package me.gegenbauer.catspy.log.ui.customize

class TableParamEditor(tooltip: String? = null): ParamEditor(tooltip) {

    init {
        isEditable = true
    }

    override fun setJIInputVerifier() {
        setTableInputVerifier(this)
    }
}