package me.gegenbauer.catspy.render.html

import org.junit.jupiter.api.Test
import java.awt.Color
import kotlin.test.assertEquals

class HtmlStringRendererTest {

    @Test
    fun `should return original string if no spans are added`() {
        val renderer = HtmlStringRenderer("test")
        val result = renderer.render()
        assertEquals("test", result)
    }

    @Test
    fun `should return correct html when add span with one character`() {
        val renderer = HtmlStringRenderer("test")
        renderer.bold(0, 0)
        val result = renderer.render()
        assertEquals("<html><span style=\"font-weight:bold;\">t</span>est</html>", result)
    }

    @Test
    fun `should return correct html when add span with multiple characters`() {
        val renderer = HtmlStringRenderer("test")
        renderer.bold(0, 1)
        val result = renderer.render()
        assertEquals("<html><span style=\"font-weight:bold;\">te</span>st</html>", result)
    }

    @Test
    fun `should return correct html when add multiple spans without intersections`() {
        val renderer = HtmlStringRenderer("test")
        renderer.bold(0, 1)
        renderer.italic(2, 3)
        val result = renderer.render()
        assertEquals("<html><span style=\"font-weight:bold;\">te</span><span style=\"font-style:italic;\">st</span></html>", result)
    }

    @Test
    fun `should return correct html when add multiple spans with intersections`() {
        val renderer = HtmlStringRenderer("abcde")
        renderer.bold(0, 3)
        renderer.italic(2, 4)
        val result = renderer.render()
        assertEquals("<html><span style=\"font-weight:bold;\">ab</span><span style=\"font-weight:bold;font-style:italic;\">cd</span><span style=\"font-style:italic;\">e</span></html>", result)
    }
}