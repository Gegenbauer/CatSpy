package me.gegenbauer.catspy.utils.file

abstract class KeyValuesFileManager: StringFileManager() {

    abstract override val fileExtension: String

    abstract fun serialize(data: Map<String, Any?>): String

    abstract fun deserialize(raw: String): Map<String, Any?>
}