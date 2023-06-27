package me.gegenbauer.catspy.log.model

interface LogItem {
    fun setHidden(hidden: Boolean)

    fun isHidden(): Boolean

    fun isFromFile(): Boolean

    fun getDisplayText(): String
}