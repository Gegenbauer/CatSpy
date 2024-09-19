package me.gegenbauer.catspy.log.filter

import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import java.util.*

/**
 * Represents a record of filters that are applied to the log.
 * The filters are stored as key-value pairs, where the key is the filter name and the value is the filter value.
 */
data class FilterRecord(
    val name: String,
    val filters: List<FilterPart>
) {

    fun isEmpty(): Boolean {
        return filters.all { !it.enabled || it.content.isEmpty() }
    }

    override fun toString(): String {
        return filters.map { it.toString() }.filter { it.isNotEmpty() }.joinToString(separator = ", ")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is FilterRecord) return false

        if (name != other.name) return false
        return other.toString() == toString()
    }

    fun equalsIgnoreName(other: FilterRecord): Boolean {
        return other.toString() == toString()
    }

    override fun hashCode(): Int {
        return Objects.hash(name, filters)
    }

    companion object {
        val EMPTY = FilterRecord(EMPTY_STRING, emptyList())
    }
}

data class FilterPart(
    val name: String,
    val enabled: Boolean,
    val content: String,
    val isLevel: Boolean = false,
    val isMatchCase: Boolean = false
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FilterPart) return false

        if (name != other.name) return false
        if (isLevel != other.isLevel) return false
        if (isMatchCase != other.isMatchCase) return false
        if (enabled != other.enabled) return false
        if (enabled && content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(name, enabled, content, isLevel, isMatchCase)
    }

    override fun toString(): String {
        return if (isMatchCase) {
            "matchCase=$enabled"
        } else {
            "$name=$content".takeIf { enabled && content.isNotEmpty() } ?: EMPTY_STRING
        }
    }
}

fun List<FilterProperty>.applyFilterRecord(filterRecord: FilterRecord) {
    forEach { property ->
        val filter = filterRecord.filters.find { it.name == property.name }
        if (filter != null) {
            property.enabled.updateValue(filter.enabled)
            val content = if (property.isLevel && filter.content.isEmpty()) {
                property.contentList.getValueNonNull().firstOrNull() ?: EMPTY_STRING
            } else {
                filter.content
            }
            property.content.updateValue(content)
            property.selectedItem.updateValue(content)
        } else {
            property.enabled.updateValue(property.columnId != FilterProperty.FILTER_ID_MATCH_CASE)
            val content = if (property.isLevel) {
                property.contentList.getValueNonNull().firstOrNull() ?: EMPTY_STRING
            } else {
                EMPTY_STRING
            }
            property.content.updateValue(content)
            property.selectedItem.updateValue(content)
        }
    }
}

fun List<FilterProperty>.toFilterRecord(name: String = EMPTY_STRING): FilterRecord {
    return FilterRecord(
        name,
        filter {
            it.enabled.getValueNonNull()
        }.map { property ->
            val content = if (property.isLevel) {
                property.content.getValueNonNull().takeIf {
                    it.isNotEmpty()
                            && it != property.contentList.getValueNonNull().firstOrNull()
                            && property.enabled.getValueNonNull()
                } ?: EMPTY_STRING
            } else {
                property.content.getValueNonNull().takeIf { it.isNotEmpty() } ?: EMPTY_STRING
            }
            FilterPart(
                property.name,
                property.enabled.getValueNonNull(),
                content,
                property.isLevel,
                property.columnId == FilterProperty.FILTER_ID_MATCH_CASE
            )
        }
    )
}