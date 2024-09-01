package me.gegenbauer.catspy.utils

import me.gegenbauer.catspy.utils.ui.hexToColor
import me.gegenbauer.catspy.utils.ui.isDark
import me.gegenbauer.catspy.utils.ui.toHex
import me.gegenbauer.catspy.utils.ui.toHtml
import java.awt.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class ColorTest {

    @Test
    fun `should return correct color string for html when passing color with alpha`() {
        val color = Color(255, 255, 255, 255)
        val html = color.toHtml()
        assertEquals("rgba(255, 255, 255, 1.00)", html)
    }

    @Test
    fun `should return correct color when passing hex string with alpha`() {
        val color = "#12345678".hexToColor()
        assertEquals(Color(52, 86, 120, 18), color)
    }

    @Test
    fun `should return correct color when passing hex string without alpha`() {
        val color = "#123456".hexToColor()
        assertEquals(Color(18, 52, 86), color)
    }

    @Test
    fun `should return correct hex string when passing color without alpha`() {
        val color = Color(18, 52, 86)
        val hex = color.toHex()
        assertEquals("#ff123456", hex)
    }

    @Test
    fun `should return correct hex string when passing color with alpha`() {
        val color = Color(18, 52, 86, 18)
        val hex = color.toHex()
        assertEquals("#12123456", hex)
    }

    @Test
    fun `should return true when color is dark`() {
        val darkColor = "#CF5B56".hexToColor()
        assertEquals(true, darkColor.isDark())
    }

    @Test
    fun `should return false when color is not dark`() {
        val lightColor = "#E9F5E6".hexToColor()
        assertEquals(false, lightColor.isDark())
    }
}