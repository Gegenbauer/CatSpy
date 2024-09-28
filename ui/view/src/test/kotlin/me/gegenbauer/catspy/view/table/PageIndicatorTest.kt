package me.gegenbauer.catspy.view.table

import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import javax.swing.JLabel
import javax.swing.JToggleButton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PageIndicatorTest {

    @Test
    fun `should return empty page navigation panel when page count is 0`() {
        val pageable = PageableTestImpl()
        val pageNavigationPanel = PageIndicator(pageable)
        pageable.pageMetadata.updateValue(PageMetadata(0, 0, 0, 0))
        assertEquals(0, pageNavigationPanel.componentCount)
    }

    @Test
    fun `should return page navigation panel with correct page buttons when page count is 1`() {
        val pageable = PageableTestImpl()
        val pageNavigationPanel = PageIndicator(pageable)
        pageable.pageMetadata.updateValue(PageMetadata(0, 1, 0, 0))
        val ellipses = pageNavigationPanel.components.filterIsInstance<JLabel>()
        val pageButtons = pageNavigationPanel.components.filterIsInstance<JToggleButton>()
        assertTrue(ellipses.isEmpty())
        assertEquals(1, pageButtons.size)
        assertEquals("0", pageButtons.first().text)
    }

    @Test
    fun `should return panel with page buttons count equals to page count when page count is less than max button count`() {
        val pageable = PageableTestImpl()
        val pageNavigationPanel = PageIndicator(pageable)
        pageable.pageMetadata.updateValue(PageMetadata(0, 7, 0, 0))
        val ellipses = pageNavigationPanel.components.filterIsInstance<JLabel>()
        val pageButtons = pageNavigationPanel.components.filterIsInstance<JToggleButton>()
        assertTrue(ellipses.isEmpty())
        assertEquals(7, pageButtons.size)
        assertEquals((0 until 7).toList(), pageButtons.map { it.text.toInt() })
    }

    @Test
    fun `should return page navigation panel with correct page buttons when page count is 16 and current page is 2`() {
        val pageable = PageableTestImpl()
        val pageNavigationPanel = PageIndicator(pageable)
        pageable.pageMetadata.updateValue(PageMetadata(2, 16, 0, 0))
        val ellipses = pageNavigationPanel.components.filterIsInstance<JLabel>()
        val pageButtons = pageNavigationPanel.components.filterIsInstance<JToggleButton>()
        assertEquals(1, ellipses.size)
        assertEquals(8, pageButtons.size)
        assertEquals(arrayListOf<Int>().apply {
            addAll((0 until 7).toList())
            add(15)
        }, pageButtons.map { it.text.toInt() })
    }

    @Test
    fun `should return page navigation panel with correct page buttons when page count is 16 and current page is 15`() {
        val pageable = PageableTestImpl()
        val pageIndicator = PageIndicator(pageable)
        pageable.pageMetadata.updateValue(PageMetadata(15, 16, 0, 0))
        val ellipses = pageIndicator.components.filterIsInstance<JLabel>()
        val pageButtons = pageIndicator.components.filterIsInstance<JToggleButton>()
        assertEquals(1, ellipses.size)
        assertEquals(8, pageButtons.size)
        assertEquals(arrayListOf<Int>().apply {
            add(0)
            addAll((9 until 16).toList())
        }, pageButtons.map { it.text.toInt() })
    }

    @Test
    fun `should return page navigation panel with correct page buttons when page count is 16 and current page is 7`() {
        val pageable = PageableTestImpl()
        val pageIndicator = PageIndicator(pageable)
        pageable.pageMetadata.updateValue(PageMetadata(7, 16, 0, 0))
        val ellipses = pageIndicator.components.filterIsInstance<JLabel>()
        val pageButtons = pageIndicator.components.filterIsInstance<JToggleButton>()
        assertEquals(2, ellipses.size)
        assertEquals(7, pageButtons.size)
        assertEquals(arrayListOf<Int>().apply {
            add(0)
            addAll((5 until 10).toList())
            add(15)
        }, pageButtons.map { it.text.toInt() })
    }

    class PageableTestImpl : Pageable<String> {
        override val pageMetadata: ObservableValueProperty<PageMetadata> = ObservableValueProperty()

        override fun nextPage() {}

        override fun previousPage() {}

        override fun firstPage() {}

        override fun lastPage() {}

        override fun gotoPage(page: Int) {}

        override fun <R> accessPageData(page: Int, action: (List<String>) -> R): R {
            return action.invoke(emptyList())
        }

    }
}