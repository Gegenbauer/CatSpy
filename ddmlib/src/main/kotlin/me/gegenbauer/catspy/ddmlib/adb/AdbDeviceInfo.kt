package me.gegenbauer.catspy.ddmlib.adb

import me.gegenbauer.catspy.ddmlib.log.DdmLog
import java.util.*

class AdbDeviceInfo(info: String, host: String, port: Int) {

    val adbHost: String
    val adbPort: Int
    val serial: String
    val state: String
    val model: String
    val allInfo: String

    val isOnline: Boolean
        get() = state == STATE_DEVICE

    /**
     * Store the device info property values like "device" "model" "product" or "transport_id"
     */
    private val properties = TreeMap<String, String>()


    init {
        adbHost = host
        adbPort = port

        val infoFields = info.split("\\s+")
        allInfo = infoFields.joinToString(" ")
        if (infoFields.size > 2) {
            serial = infoFields[0]
            state = infoFields[1]

            properties.putAll(parseProperties(infoFields))
            model = properties.getOrDefault(KEY_MODEL, serial)
        } else {
            DdmLog.e(TAG, "[parseDeviceInfo] invalid device info: $info")

            serial = ""
            state = STATE_UNKNOWN
            model = STATE_UNKNOWN
        }
    }

    private fun parseProperties(fields: List<String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (i in 2 until fields.size) {
            val field = fields[i]
            val idx = field.indexOf(KEY_VALUE_SEPARATOR)
            if (idx > 0) {
                val key = field.substring(0, idx)
                val value = field.substring(idx + 1)
                if (value.isNotEmpty()) {
                    map[key] = value
                }
            }
        }
        return map
    }

    fun getProperty(key: String): String? {
        return properties[key]
    }

    companion object {
        private const val TAG = "AdbDevice"

        private const val KEY_MODEL = "model"
        private const val KEY_VALUE_SEPARATOR = ':'

        private const val STATE_DEVICE = "device"
        private const val STATE_UNKNOWN = "unknown"
    }
}