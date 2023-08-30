package me.gegenbauer.catspy.view.table

import me.gegenbauer.catspy.databinding.bind.ObservableViewModelProperty

interface Pageable<T> {

    val pageMetaData: ObservableViewModelProperty<PageMetadata>

    val pageCount: Int
        get() = pageMetaData.value?.pageCount ?: 0

    val currentPage: Int
        get() = pageMetaData.value?.currentPage ?: 0

    val pageSize: Int
        get() = pageMetaData.value?.pageSize ?: 0

    val dataSize: Int
        get() = pageMetaData.value?.dataSize ?: 0

    fun nextPage()

    fun previousPage()

    fun firstPage()

    fun lastPage()

    fun gotoPage(page: Int)

    fun <R> accessPageData(page: Int, action: (List<T>) -> R): R
}

data class PageMetadata(
    val currentPage: Int = -1,
    val pageCount: Int = 0,
    val pageSize: Int = 0,
    val dataSize: Int = 0
) {
    fun isIndicatorDataEquals(other: PageMetadata): Boolean {
        return currentPage == other.currentPage
                && pageCount == other.pageCount
    }

    fun isPageChanged(other: PageMetadata): Boolean {
        return currentPage > 0 && currentPage != other.currentPage
    }
}