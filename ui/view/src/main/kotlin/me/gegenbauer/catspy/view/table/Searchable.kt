package me.gegenbauer.catspy.view.table

import me.gegenbauer.catspy.view.filter.FilterItem

interface Searchable {
    var searchFilterItem: FilterItem

    /**
     * Moves to the next search result
     * @return Result of the operation, if no result is found, an error string is returned
     */
    fun moveToNextSearchResult(): String

    /**
     * Moves to the previous search result
     * @return Result of the operation, if no result is found, an error string is returned
     */
    fun moveToPreviousSearchResult(): String
}
