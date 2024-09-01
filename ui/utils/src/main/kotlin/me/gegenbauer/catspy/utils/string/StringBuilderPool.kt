package me.gegenbauer.catspy.utils.string

import me.gegenbauer.catspy.cache.CacheableObject
import me.gegenbauer.catspy.cache.ObjectPool

interface SimpleStringBuilder {
    fun set(text: String): SimpleStringBuilder

    fun append(text: String): SimpleStringBuilder

    fun appendLine(text: String) : SimpleStringBuilder

    fun appendLine() : SimpleStringBuilder

    fun clear() : SimpleStringBuilder

    fun replace(regex: Regex, newValue: String) : SimpleStringBuilder

    fun substring(startIndex: Int, endIndex: Int): SimpleStringBuilder

    fun substring(startIndex: Int): SimpleStringBuilder

    fun length(): Int

    fun isEmpty(): Boolean

    fun build(): String
}

class CacheableStringBuilder: SimpleStringBuilder, CacheableObject {
    private val cache = StringBuilder()

    override fun set(text: String): SimpleStringBuilder {
        cache.setLength(0)
        cache.append(text)
        return this
    }

    override fun append(text: String) : SimpleStringBuilder {
        cache.append(text)
        return this
    }

    override fun appendLine(text: String): SimpleStringBuilder {
        cache.appendLine(text)
        return this
    }

    override fun appendLine(): SimpleStringBuilder {
        cache.appendLine()
        return this
    }

    override fun clear(): SimpleStringBuilder {
        cache.clear()
        return this
    }

    override fun replace(regex: Regex, newValue: String): SimpleStringBuilder {
        val matcher = regex.toPattern().matcher(cache)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            cache.replace(start, end, newValue)
        }
        return this
    }

    override fun substring(startIndex: Int, endIndex: Int): SimpleStringBuilder {
        cache.deleteRange(endIndex, cache.length)
        cache.deleteRange(0, startIndex)
        return this
    }

    override fun substring(startIndex: Int): SimpleStringBuilder {
        cache.deleteRange(0, startIndex)
        return this
    }

    override fun length(): Int {
        return cache.length
    }

    override fun isEmpty(): Boolean {
        return cache.isEmpty()
    }

    override fun build(): String {
        return cache.toString()
    }

    override fun recycle() {
        cache.clear()
        pool.recycle(this)
    }

    companion object {
        private val pool = ObjectPool.createMemoryAwarePool(100) { CacheableStringBuilder() }

        fun obtain(): CacheableStringBuilder {
            return pool.obtain()
        }
    }
}