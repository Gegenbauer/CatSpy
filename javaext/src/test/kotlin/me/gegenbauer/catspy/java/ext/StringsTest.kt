package me.gegenbauer.catspy.java.ext

import kotlin.test.Test
import kotlin.test.assertEquals

class StringsTest {

    @Test
    fun `should capitalize first letter of string`() {
        val input = "hello"
        val expected = "Hello"
        assertEquals(expected, input.capitalize())
    }

    @Test
    fun `should truncate string at end`() {
        val input = "hello world"
        val expected = "hello..."
        assertEquals(expected, input.truncate(8))
    }

    @Test
    fun `should truncate string at start`() {
        val input = "hello world"
        val expected = "...world"
        assertEquals(expected, input.truncate(8, EllipsisPosition.START))
    }

    @Test
    fun `should truncate string in middle`() {
        val input = "hello world"
        val expected = "hell...rld"
        assertEquals(expected, input.truncate(10, EllipsisPosition.MIDDLE))
    }
}