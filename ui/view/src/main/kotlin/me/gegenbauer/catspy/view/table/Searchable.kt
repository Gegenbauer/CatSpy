package me.gegenbauer.catspy.view.table

import me.gegenbauer.catspy.view.filter.FilterItem

interface Searchable {
    var searchFilterItem: FilterItem

    var searchMatchCase: Boolean

    fun moveToNextSearchResult()

    fun moveToPreviousSearchResult()
}