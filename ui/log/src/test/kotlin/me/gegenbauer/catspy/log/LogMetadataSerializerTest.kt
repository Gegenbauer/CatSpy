package me.gegenbauer.catspy.log

import me.gegenbauer.catspy.common.Resources
import me.gegenbauer.catspy.log.serialize.LogMetadataSerializer
import kotlin.test.Test
import kotlin.test.assertEquals

class LogMetadataSerializerTest {

    @Test
    fun `should return correct metadata when parse common metadata json file`() {
        val jsonFilePath = "standard_logcat_device_log_metadata.json"
        val json = Resources.loadResourceAsStream(jsonFilePath).readBytes().decodeToString()
        val metadata = LogMetadataSerializer().deserialize(json)
        assertEquals(1, metadata.version)
        assertEquals("StandardLogcatDeviceLog", metadata.logType)
        assertEquals(7, metadata.columns.size)
        assertEquals(6, metadata.levels.size)
    }
}