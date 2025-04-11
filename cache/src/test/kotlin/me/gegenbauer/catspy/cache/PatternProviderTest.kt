package me.gegenbauer.catspy.cache

import me.gegenbauer.catspy.context.MemoryAware
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PatternProviderTest {

    @Test
    fun `should create new pattern when cache is empty`() {
        // Given
        val patternProvider = PatternProvider()

        // When
        val pattern = patternProvider[PatternKey("test")]

        // Then
        assertEquals("test", pattern?.pattern())
    }

    @Test
    fun `should return cached pattern when requested key is already in cache`() {
        // Given
        val patternProvider = PatternProvider()
        val patternKey = PatternKey("test")
        val pattern = patternProvider[patternKey]

        // When
        val cachedPattern = patternProvider[patternKey]

        // Then
        assertTrue { pattern === cachedPattern }
    }

    @Test
    fun `should keep cache size below max size`() {
        // Given
        val patternProvider = PatternProvider(1)
        val patternKey1 = PatternKey("test1")
        val patternKey2 = PatternKey("test2")

        // When
        patternProvider[patternKey1]
        patternProvider[patternKey2]

        // Then
        assertEquals(1, patternProvider.count)
    }

    @Test
    fun `should remove pattern from cache when it is cleared`() {
        // Given
        val patternProvider = PatternProvider()
        val patternKey = PatternKey("test")
        val pattern = patternProvider[patternKey]

        // When
        patternProvider.clearMemory()

        // Then
        assertEquals(0, patternProvider.count)
    }

    @Test
    fun `should remove pattern from cache when it is removed`() {
        // Given
        val patternProvider = PatternProvider()
        val patternKey = PatternKey("test")
        val pattern = patternProvider[patternKey]

        // When
        patternProvider.remove(patternKey)

        // Then
        assertEquals(0, patternProvider.count)
    }

    @Test
    fun `should remove longest unused pattern from cache when cache is full`() {
        // Given
        val patternProvider = PatternProvider(4)
        val patternKey1 = PatternKey("test1")
        val patternKey2 = PatternKey("test2")
        val patternKey3 = PatternKey("test3")
        val patternKey4 = PatternKey("test4")
        val patternKey5 = PatternKey("test5")

        // When
        val pattern1 = patternProvider[patternKey1]
        val pattern2 = patternProvider[patternKey2]
        val pattern3 = patternProvider[patternKey3]
        val pattern4 = patternProvider[patternKey4]
        val pattern5 = patternProvider[patternKey5]

        // Then
        assertEquals(4, patternProvider.count)
        assertSame(patternProvider[patternKey2], pattern2)
        assertSame(patternProvider[patternKey3], pattern3)
        assertSame(patternProvider[patternKey4], pattern4)
        assertSame(patternProvider[patternKey5], pattern5)
        assertTrue { patternProvider[patternKey1] !== pattern1 }
    }

    @Test
    fun `should decrease cache size when memory is trimmed`() {
        // Given
        val patternProvider = PatternProvider(5)
        patternProvider[PatternKey("test1")]
        patternProvider[PatternKey("test2")]
        patternProvider[PatternKey("test3")]
        patternProvider[PatternKey("test4")]
        patternProvider[PatternKey("test5")]

        // When
        patternProvider.onTrimMemory(MemoryAware.Level.HIGH)

        // Then
        assertEquals(4, patternProvider.count)
    }
}