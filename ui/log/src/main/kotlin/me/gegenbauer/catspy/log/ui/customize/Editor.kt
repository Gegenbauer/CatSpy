package me.gegenbauer.catspy.log.ui.customize

interface Editor {

    fun startEditing()

    fun stopEditing()

    fun isEditValid(): Boolean
}

interface EditableContainer: Editor {
    /**
     * Whether it is currently in edit mode, as a cache,
     * the edit state of newly created child controls should be consistent with it
     */
    val isEditing: Boolean

    override fun isEditValid(): Boolean {
        return true
    }
}