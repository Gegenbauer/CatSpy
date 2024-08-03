package me.gegenbauer.catspy.log.recent

data class RecentFile(
    val name: String = "",
    val path: String = "",
    val lastOpenTime: Long = System.currentTimeMillis(),
    val isStarred: Boolean = false
) {
    constructor(filePath: String) : this(
        name = filePath.substringAfterLast("/"),
        path = filePath
    )
}