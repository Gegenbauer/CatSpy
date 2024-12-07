package me.gegenbauer.catspy.log.metadata

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.java.ext.getUniqueName
import me.gegenbauer.catspy.log.serialize.LogMetadataModel
import me.gegenbauer.catspy.log.serialize.LogMetadataModelSerializer
import me.gegenbauer.catspy.log.serialize.LogMetadataSerializer
import me.gegenbauer.catspy.log.serialize.toLogMetadata
import me.gegenbauer.catspy.log.serialize.toLogMetadataModel
import me.gegenbauer.catspy.platform.filesDir
import me.gegenbauer.catspy.utils.file.JsonFileManager
import me.gegenbauer.catspy.utils.file.StringFileManager.Companion.JSON_PREFIX
import java.io.File
import java.lang.ref.WeakReference

interface LogMetadataChangeListener {
    fun onMetadataChanged(old: LogMetadataModel, new: LogMetadataModel) {}

    fun onMetadataAdded(metadata: LogMetadataModel) {}

    fun onMetadataDeleted(logType: String) {}
}

interface ILogMetadataManager {

    /**
     * Load all configuration information, including built-in and user-defined.
     * Only needs to be called once during initialization.
     */
    suspend fun loadAllMetadata(): List<LogMetadata>

    /**
     * Do not pass in an instance of an anonymous inner class created in a method,
     * it will be recycled after the method is completed.
     */
    fun addOnMetadataChangeListener(listener: LogMetadataChangeListener)

    fun isCustomized(logType: String): Boolean

    fun resetToBuiltIn(logType: String): LogMetadataModel

    fun getMetadata(logType: String): LogMetadataModel?

    fun getDeviceLogMetadata(): LogMetadataModel

    fun delete(logType: String)

    fun getUniqueLogType(logType: String): String

    fun addNewLogMetadata(logMetadataModel: LogMetadataModel)

    fun modifyLogMetadata(old: LogMetadataModel, new: LogMetadataModel)

    fun getAllFileLogMetadata(): List<LogMetadataModel>
}

class LogMetadataManager : ContextService, ILogMetadataManager {
    private val dispatcher = Dispatchers.GIO

    private val builtInMetadataGroup = mutableMapOf<String, LogMetadataModel>()
    private val customizedMetadataGroup = mutableMapOf<String, MetadataWithFilename>()
    private val mergedMetadataGroup = mutableMapOf<String, LogMetadataModel>()

    private val logMetadataModelSerializer = LogMetadataModelSerializer()

    private val metadataDir: File
        get() = File(filesDir.appendPath(CUSTOMIZED_METADATA_FILE_PATH))

    private val jsonFileManager: JsonFileManager
        get() = ServiceManager.getContextService(JsonFileManager::class.java)
    private val onLogMetadataChangeListener = mutableListOf<WeakReference<LogMetadataChangeListener>>()

    private val builtInMetadataProviders = listOf(
        StandardLogcatFileLogMetadataProvider(),
        StandardDeviceLogMetadataProvider(),
        DefaultRawLogMetadataProvider(),
        TimeFormatFileLogMetadataProvider()
    )

    override suspend fun loadAllMetadata(): List<LogMetadata> {
        val builtInMetadata = loadBuiltInMetadata()
        updateBuiltInCacheAll(builtInMetadata)

        val logMetadataMap = builtInMetadata.associateBy { it.logType }.toMutableMap()
        val customizedMetadata = loadCustomizedMetadata()
        updateCustomizedCacheAll(customizedMetadata)

        customizedMetadata.forEach {
            logMetadataMap[it.metadata.logType] = it.metadata.copy(isBuiltIn = true)
                .takeIf { logMetadataMap.containsKey(it.logType) }
                ?: it.metadata
        }
        updateMergedCacheAll(logMetadataMap.values.toList())

        return logMetadataMap.values.map { it.toLogMetadata() }
    }

    private suspend fun loadBuiltInMetadata(): List<LogMetadataModel> {
        return withContext(dispatcher) {
            builtInMetadataProviders.map { it.getMetadata().toLogMetadataModel() }
        }
    }

    private suspend fun loadCustomizedMetadata(): List<MetadataWithFilename> {
        return withContext(dispatcher) {
            metadataDir.mkdirs()
            metadataDir.listFiles()?.filter { it.extension == "json" }?.mapNotNull { file ->
                runCatching {
                    val jsonObject = JsonParser.parseString(file.readText()).asJsonObject
                    val migratedJsonObject = LogMetadataMigrations.migrate(jsonObject, LogMetadata.VERSION)
                    if (migratedJsonObject == null) {
                        GLog.w(TAG, "Failed to migrate metadata file: ${file.name}, discard it")
                        return@runCatching null
                    }
                    MetadataWithFilename(
                        LogMetadataSerializer().deserialize(file.readText()).toLogMetadataModel(),
                        file.nameWithoutExtension
                    )
                }.onFailure {
                    GLog.w(TAG, "Failed to load customized metadata from file: ${file.name}", it)
                }.getOrNull()
            } ?: emptyList()
        }
    }

    @Synchronized
    override fun isCustomized(logType: String): Boolean {
        return customizedMetadataGroup.containsKey(logType)
    }

    @SuppressWarnings("UNCHECKED_CAST")
    @Synchronized
    override fun resetToBuiltIn(logType: String): LogMetadataModel {
        val builtInMetadata =
            builtInMetadataGroup[logType] ?: throw IllegalArgumentException("No built-in metadata found for $logType")
        val customizedMetadata = customizedMetadataGroup[logType]
            ?: throw IllegalArgumentException("No customized metadata found for $logType")
        deleteInternal(logType)
        notifyMetadataChange(customizedMetadata.metadata, builtInMetadata)
        return builtInMetadata
    }

    @Synchronized
    override fun getMetadata(logType: String): LogMetadataModel? {
        return mergedMetadataGroup[logType]
    }

    @Synchronized
    override fun getDeviceLogMetadata(): LogMetadataModel {
        return mergedMetadataGroup.values.first { it.isDeviceLog }
    }

    override fun delete(logType: String) {
        deleteInternal(logType)
        notifyMetadataDeleted(logType)
    }

    private fun deleteInternal(logType: String) {
        val filename = customizedMetadataGroup[logType]?.filename
        deleteCache(logType)
        filename?.let {
            jsonFileManager.delete(CUSTOMIZED_METADATA_FILE_PATH.appendPath(it.removePrefix(JSON_PREFIX)))
        }
    }

    override fun getUniqueLogType(logType: String): String {
        val usedNames = (customizedMetadataGroup.values.map { it.metadata }.toSet() + builtInMetadataGroup.values)
            .map { it.logType }
            .toSet()
        return getUniqueName(logType, usedNames)
    }

    override fun addNewLogMetadata(logMetadataModel: LogMetadataModel) {
        save(logMetadataModel)
        notifyMetadataAdded(logMetadataModel)
    }

    private fun save(logMetadataModel: LogMetadataModel) {
        updateCache(logMetadataModel)
        metadataDir.mkdirs()
        jsonFileManager.write(
            generateJsonFileKey(logMetadataModel.logType),
            logMetadataModelSerializer.serialize(logMetadataModel)
        )
    }

    override fun modifyLogMetadata(old: LogMetadataModel, new: LogMetadataModel) {
        if (old.logType != new.logType) {
            deleteInternal(old.logType)
        }
        save(new)
        notifyMetadataChange(old, new)
    }

    @Synchronized
    override fun getAllFileLogMetadata(): List<LogMetadataModel> {
        return mergedMetadataGroup.values.filter { it.isDeviceLog.not() }.toList()
    }

    override fun addOnMetadataChangeListener(listener: LogMetadataChangeListener) {
        onLogMetadataChangeListener.add(WeakReference(listener))
    }

    private fun notifyMetadataChange(old: LogMetadataModel, new: LogMetadataModel) {
        onLogMetadataChangeListener.forEach {
            it.get()?.onMetadataChanged(old, new)
        }
    }

    private fun notifyMetadataAdded(metadata: LogMetadataModel) {
        onLogMetadataChangeListener.forEach {
            it.get()?.onMetadataAdded(metadata)
        }
    }

    private fun notifyMetadataDeleted(logType: String) {
        onLogMetadataChangeListener.forEach {
            it.get()?.onMetadataDeleted(logType)
        }
    }

    private fun generateJsonFileKey(key: String): String {
        return CUSTOMIZED_METADATA_FILE_PATH.appendPath(key.hashCode().toString())
    }

    @Synchronized
    private fun updateCache(logMetadata: LogMetadataModel) {
        val existingMetadata = customizedMetadataGroup[logMetadata.logType]
        customizedMetadataGroup[logMetadata.logType] =
            MetadataWithFilename(logMetadata, existingMetadata?.filename ?: "")
        mergedMetadataGroup[logMetadata.logType] = logMetadata
    }

    @Synchronized
    private fun deleteCache(key: String) {
        customizedMetadataGroup.remove(key)

        mergedMetadataGroup.remove(key)
        if (builtInMetadataGroup.containsKey(key)) {
            mergedMetadataGroup[key] = builtInMetadataGroup[key]
                ?: throw IllegalStateException("Built-in metadata not found")
        }
    }

    @Synchronized
    private fun updateCustomizedCacheAll(metadata: List<MetadataWithFilename>) {
        customizedMetadataGroup.clear()
        customizedMetadataGroup.putAll(metadata.associateBy { it.metadata.logType })
    }

    @Synchronized
    private fun updateBuiltInCacheAll(metadata: List<LogMetadataModel>) {
        builtInMetadataGroup.clear()
        builtInMetadataGroup.putAll(metadata.associateBy { it.logType })
    }

    @Synchronized
    private fun updateMergedCacheAll(metadata: List<LogMetadataModel>) {
        mergedMetadataGroup.clear()
        mergedMetadataGroup.putAll(metadata.associateBy { it.logType })
    }

    private data class MetadataWithFilename(var metadata: LogMetadataModel, val filename: String)

    companion object {
        private const val TAG = "LogMetadataManager"

        const val LOG_TYPE_RAW = "DefaultRawLog"
        const val LOG_TYPE_DEVICE = "StandardLogcatDeviceLog"

        private const val CUSTOMIZED_METADATA_FILE_PATH = "metadata"
    }
}