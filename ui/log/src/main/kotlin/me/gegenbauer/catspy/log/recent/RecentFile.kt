package me.gegenbauer.catspy.log.recent

import me.gegenbauer.catspy.file.fileName
import me.gegenbauer.catspy.java.ext.EMPTY_STRING

data class RecentFile(
    val name: String = EMPTY_STRING,
    val path: String = EMPTY_STRING,
    val lastOpenTime: Long = System.currentTimeMillis(),
    val isStarred: Boolean = false
) {
    constructor(filePath: String) : this(
        name = filePath.fileName,
        path = filePath
    )
}