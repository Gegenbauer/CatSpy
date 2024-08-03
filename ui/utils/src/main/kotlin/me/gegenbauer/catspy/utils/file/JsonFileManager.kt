package me.gegenbauer.catspy.utils.file

class JsonFileManager: StringFileManager() {

    override val fileExtension: String = FILE_EXTENSION

    companion object {
        const val FILE_EXTENSION = "json"
    }
}