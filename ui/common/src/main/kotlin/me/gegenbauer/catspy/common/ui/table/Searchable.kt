package me.gegenbauer.catspy.common.ui.table

import me.gegenbauer.catspy.common.log.FilterItem

interface Searchable {
    var searchFilterItem: FilterItem

    var searchMatchCase: Boolean

    fun moveToNextSearchResult()

    fun moveToPreviousSearchResult()
}