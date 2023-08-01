package me.gegenbauer.catspy.common.ui.table

import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty

interface Pageable<T> {

    val observablePageMetaData: ObservableViewModelProperty<PageMetadata>

    val pageCount: Int
        get() = observablePageMetaData.value?.pageCount ?: 0

    val currentPage: Int
        get() = observablePageMetaData.value?.currentPage ?: 0

    val pageSize: Int
        get() = observablePageMetaData.value?.pageSize ?: 0

    val dataSize: Int
        get() = observablePageMetaData.value?.dataSize ?: 0

    fun nextPage()

    fun previousPage()

    fun firstPage()

    fun lastPage()

    fun gotoPage(page: Int)

    fun <R> accessPageData(page: Int, action: (List<T>) -> R): R
}

data class PageMetadata(
    val currentPage: Int,
    val pageCount: Int,
    val pageSize: Int,
    val dataSize: Int
)