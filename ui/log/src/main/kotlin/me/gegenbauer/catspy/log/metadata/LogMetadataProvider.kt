package me.gegenbauer.catspy.log.metadata

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.common.Resources
import me.gegenbauer.catspy.concurrency.GIO
import me.gegenbauer.catspy.log.serialize.LogMetadataSerializer

fun interface LogMetadataProvider {

    suspend fun getMetadata(): LogMetadata
}

abstract class BuiltInLogMetadataProvider(protected val dispatcher: CoroutineDispatcher) : LogMetadataProvider

abstract class StandardLogcatLogMetadataProvider(dispatcher: CoroutineDispatcher) :
    BuiltInLogMetadataProvider(dispatcher) {

    abstract val metadataFilePath: String

    override suspend fun getMetadata(): LogMetadata {
        return withContext(dispatcher) {
            val buildInMetadataJson =
                Resources.loadResourceAsStream(metadataFilePath).readBytes().decodeToString()
            serializer.deserialize(buildInMetadataJson)
        }
    }

    companion object {
        private val serializer = LogMetadataSerializer()
    }
}

class StandardLogcatFileLogMetadataProvider(dispatcher: CoroutineDispatcher = Dispatchers.GIO) :
    StandardLogcatLogMetadataProvider(dispatcher) {

    override val metadataFilePath: String = LOG_METADATA_FILE_PATH

    companion object {
        private const val LOG_METADATA_FILE_PATH = "log/metadata/standard_logcat_file_log_metadata.json"
    }
}

class StandardDeviceLogMetadataProvider(dispatcher: CoroutineDispatcher = Dispatchers.GIO) :
    StandardLogcatLogMetadataProvider(dispatcher) {

    override val metadataFilePath: String = LOG_METADATA_FILE_PATH

    companion object {
        private const val LOG_METADATA_FILE_PATH = "log/metadata/standard_logcat_device_log_metadata.json"
    }
}

class DefaultRawLogMetadataProvider(dispatcher: CoroutineDispatcher = Dispatchers.GIO) :
    StandardLogcatLogMetadataProvider(dispatcher) {

    override val metadataFilePath: String = LOG_METADATA_FILE_PATH

    companion object {
        private const val LOG_METADATA_FILE_PATH = "log/metadata/raw_log_metadata.json"
    }
}

class TimeFormatFileLogMetadataProvider(dispatcher: CoroutineDispatcher = Dispatchers.GIO) :
    StandardLogcatLogMetadataProvider(dispatcher) {

    override val metadataFilePath: String = LOG_METADATA_FILE_PATH

    companion object {
        private const val LOG_METADATA_FILE_PATH = "log/metadata/time_format_logcat_file_log.json"
    }
}