package me.gegenbauer.catspy.render

import java.util.*

class TaggedStringBuilder {

    private val tags = Stack<Tag>()
    private val cache = StringBuilder()

    fun addSingleTag(tag: Tag) {
        cache.append("<${tag.value}/>")
    }

    fun addTag(tag: Tag, property: String = "") {
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

    fun closeTag() {
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

    fun isAnyTagOpen(): Boolean {
        return tags.isNotEmpty()
    }

    fun build(): String {
        return cache.toString()
    }

}

enum class Tag(val value: String) {
    SPAN("span"),
    HTML("html"),
    BODY("body"),
    PARAGRAPH("p"),
    LINE_BREAK("br"),
}