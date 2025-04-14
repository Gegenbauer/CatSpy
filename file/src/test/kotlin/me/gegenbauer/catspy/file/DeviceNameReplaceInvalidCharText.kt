package me.gegenbauer.catspy.file

import kotlin.test.Test

class DeviceNameReplaceInvalidCharText {

    @Test
    fun `should replace invalid characters when input string contains invalid chars`() {
        val deviceName = "abcdef:? asdas/$ "
        val expected = "abcdef___asdas___"

        val result = getValidFileName(deviceName)

        assert(result == expected) { "Expected $expected but got $result" }
    }
}