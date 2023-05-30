package me.gegenbauer.catspy.ui.combobox

class HistoryItem<T>(
    val content: T,
    private val lastUsed: Long = System.currentTimeMillis()
) {
    operator fun compareTo(other: HistoryItem<T>): Int {
        return lastUsed.compareTo(other.lastUsed)
    }

    override fun toString(): String {
        return content.toString()
    }

    override fun hashCode(): Int {
        return content.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is HistoryItem<*> && other.content == content
    }

    companion object {
        val comparator = compareBy<HistoryItem<*>> { it.lastUsed }.reversed()
    }
}

fun <T> List<HistoryItem<T>>.toContentList(): List<T> {
    return map { it.content }
}

fun <T> List<T>.toHistoryItemList(): List<HistoryItem<T>> {
    return map { HistoryItem(it) }
}