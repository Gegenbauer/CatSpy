package me.gegenbauer.catspy.ui.log

interface LogItem {
    fun setHidden(hidden: Boolean)

    fun isHidden(): Boolean

    fun isFromFile(): Boolean

    fun getDisplayText(): String
}