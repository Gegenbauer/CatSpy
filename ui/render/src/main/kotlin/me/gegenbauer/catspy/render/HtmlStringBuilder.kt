package me.gegenbauer.catspy.render

import me.gegenbauer.catspy.cache.CacheableObject
import me.gegenbauer.catspy.cache.ObjectPool
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import java.util.*

class HtmlStringBuilder private constructor() : CacheableObject {

    private val tags = Stack<Tag>()
    private val cache = StringBuilder()

    var isHtmlTagInitialized: Boolean = false
        set(value) {
            tags.clear()
            if (value) {
                addTag(Tag.HTML)
                addTag(Tag.BODY)
            }
            field = value
        }

    fun addSingleTag(tag: Tag) {
        cache.append("<${tag.value}/>")
    }

    fun addTag(tag: Tag, property: String = EMPTY_STRING) {
        tags.push(tag)
        cache.append("<${tag.value}")
        if (property.isBlank().not()) {
            cache.append(" $property")
        }
        cache.append(">")
    }

    fun append(text: String) {
        cache.append(text)
    }

    private fun closeTag() {
        tags.pop().apply {
            cache.append("</${this.value}>")
        }
    }

    fun closeTag(tag: Tag) {
        tags.pop().apply {
            if (this != tag) {
                return
            }
            cache.append("</${this.value}>")
        }
    }

    private fun closeAllTags() {
        while (tags.isNotEmpty()) {
            closeTag()
        }
    }

    fun build(): String {
        closeAllTags()
        return cache.toString()
    }

    override fun recycle() {
        cache.clear()
        tags.clear()
        isHtmlTagInitialized = false
        pool.recycle(this)
    }

    companion object {
        private val pool = ObjectPool.createMemoryAwarePool(1000) { HtmlStringBuilder() }

        fun obtain(): HtmlStringBuilder {
            return pool.obtain()
        }
    }
}

enum class Tag(val value: String) {
    SPAN("span"),
    HTML("html"),
    BODY("body"),
    PARAGRAPH("p"),
    LINE_BREAK("br"),
}