package me.gegenbauer.catspy.view.table

import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty

interface Pageable<T> {

    val pageMetadata: ObservableValueProperty<PageMetadata>

    val pageCount: Int
        get() = pageMetadata.value?.pageCount ?: 0

    val currentPage: Int
        get() = pageMetadata.value?.currentPage ?: 0

    val pageSize: Int
        get() = pageMetadata.value?.pageSize ?: 0

    val dataSize: Int
        get() = pageMetadata.value?.dataSize ?: 0

    fun nextPage()

    fun previousPage()

    fun firstPage()

    fun lastPage()

    fun gotoPage(page: Int)

    fun <R> accessPageData(page: Int, action: (List<T>) -> R): R
}

inline val Pageable<*>.isAtLastPage: Boolean
    get() = currentPage == pageCount - 1

inline val Pageable<*>.lastPage: Int
    get() = pageCount - 1

inline val PageMetadata.isAtLastPage: Boolean
    get() = pageCount == 0 || currentPage == pageCount - 1

data class PageMetadata(
    val currentPage: Int = 0,
    val pageCount: Int = 0,
    val pageSize: Int = 0,
    val dataSize: Int = 0
) {

    fun isIndicatorDataEquals(other: PageMetadata): Boolean {
        return currentPage == other.currentPage
                && pageCount == other.pageCount
    }
}