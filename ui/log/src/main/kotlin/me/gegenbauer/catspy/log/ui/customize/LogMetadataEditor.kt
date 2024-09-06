package me.gegenbauer.catspy.log.ui.customize

interface LogMetadataEditor: Editor {
    fun setLogMetadata(metadata: LogMetadataEditModel)

    fun onNightModeChanged(isDark: Boolean) {}

    fun isModified(): Boolean
}
