package me.gegenbauer.catspy.view.table

import me.gegenbauer.catspy.view.filter.FilterItem

interface Searchable {
    var searchFilterItem: FilterItem

    fun moveToNextSearchResult()

    fun moveToPreviousSearchResult()
}