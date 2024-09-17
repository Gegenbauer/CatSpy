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
        return filters.map {
            if (it.enabled && it.content.isNotEmpty()) "${it.name}=${it.content}" else EMPTY_STRING
        }.filter { it.isNotEmpty() }.joinToString(", ")
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
    val content: String
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FilterPart) return false

        if (name != other.name) return false
        if (enabled != other.enabled) return false
        if (enabled && content != other.content) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(name, enabled, content)
    }
}

fun List<FilterProperty>.applyFilterRecord(filterRecord: FilterRecord) {
    forEach { property ->
        val filter = filterRecord.filters.find { it.name == property.name }
        if (filter != null) {
            property.enabled.updateValue(filter.enabled)
            property.content.updateValue(filter.content)
        } else {
            property.enabled.updateValue(false)
            property.content.updateValue(EMPTY_STRING)
        }
    }
}

fun List<FilterProperty>.toFilterRecord(name: String = EMPTY_STRING): FilterRecord {
    return FilterRecord(name, map { property ->
        FilterPart(
            property.name,
            property.enabled.getValueNonNull(),
            property.content.getValueNonNull().takeIf { it.isNotEmpty() } ?: EMPTY_STRING
        )
    })
}