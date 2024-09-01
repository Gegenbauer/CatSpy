package me.gegenbauer.catspy.log.ui.customize

import me.gegenbauer.catspy.log.serialize.LogMetadataModel
import java.util.concurrent.atomic.AtomicInteger

data class LogMetadataEditModel(
    val model: LogMetadataModel,
    val isEdited: Boolean,
    val isNew: Boolean,
    val isDeleted: Boolean = false,
    val isNightMode: Boolean = false,
    val id: Int
) {

    companion object {
        private val id = AtomicInteger(0)

        fun generateNewId() = id.incrementAndGet()
    }
}

fun LogMetadataModel.toEditModel(
    isEdited: Boolean = false,
    isNew: Boolean = false,
    isDeleted: Boolean = false,
    isNightMode: Boolean = false,
    id: Int = LogMetadataEditModel.generateNewId()
) = LogMetadataEditModel(this, isEdited, isNew, isDeleted, isNightMode, id)

fun createNewLogMetadataEditModel(id: Int = LogMetadataEditModel.generateNewId()) =
    LogMetadataModel.default.toEditModel(isNew = true, id = id)