package me.gegenbauer.catspy.view.table

import java.awt.Component
import java.awt.Dimension
import java.awt.Insets
import java.awt.event.ActionEvent
import javax.swing.ButtonGroup
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JToggleButton
import javax.swing.SwingConstants

class PageIndicator<T>(
    private val pageable: Pageable<T>
) : JPanel(), Pageable<T> by pageable {

    private var lastPageMetaData: PageMetadata = PageMetadata()

    init {
        minimumSize = Dimension(0, 0)
        refreshPageNavigationButtons()
        pageable.pageMetadata.addObserver { pageMetaData ->
            pageMetaData ?: return@addObserver

            if (lastPageMetaData.isIndicatorDataEquals(pageMetaData)) {
                return@addObserver
            }

            refreshPageNavigationButtons(pageMetaData.pageCount, pageMetaData.currentPage)

            lastPageMetaData = pageMetaData
        }
    }

    /**
     * Given MAX_PAGE_BUTTON_COUNT = 5, pageCount = 10
     * 0, 1, 2, 3, 4, 5, 6, 7, 8, 9
     */
    private fun refreshPageNavigationButtons(pageCount: Int = 0, currentPage: Int = 0) {
        removeAll()

        if (pageCount <= 1) return

        val buttonGroup = ButtonGroup()
        if (pageCount > DEFAULT_MAX_PAGE_BUTTON_COUNT) {  // 0, ..., k, k+1, k+2, k+3, k+4, ..., n - 1
            addPageButtonRange(buttonGroup, 0, 1, currentPage) // add first page button
            // if ellipses has no more than 2 buttons, then add ellipses
            val ellipsesButtons = getEllipsesComponentCount(pageCount, currentPage)
            // add ellipses component when ellipsesCount > 1
            addEllipsesComponentOrButton(ellipsesButtons.first, buttonGroup, 1, currentPage)

            // add middle page buttons
            val middleButtonEachSide = getMiddleButtonEachSide()
            if (ellipsesButtons.first == 0) {
                addPageButtonRange(buttonGroup, 2, 2 + DEFAULT_MAX_PAGE_BUTTON_COUNT - 4, currentPage)
            } else if (ellipsesButtons.second == 0) {
                addPageButtonRange(buttonGroup, pageCount - 2 - DEFAULT_MAX_PAGE_BUTTON_COUNT + 4, pageCount - 2, currentPage)
            } else {
                addPageButtonRange(buttonGroup, currentPage - middleButtonEachSide, currentPage + middleButtonEachSide + 1, currentPage)
            }

            addEllipsesComponentOrButton(ellipsesButtons.second, buttonGroup, pageCount - 2, currentPage)

            addPageButtonRange(buttonGroup, pageCount - 1, pageCount, currentPage)
        } else {
            // 0, ..., n - 1, buttonCount = pageCount
            addPageButtonRange(buttonGroup, 0, pageCount, currentPage)
        }
        revalidate()
        parent?.repaint()
    }

    private fun addEllipsesComponentOrButton(ellipsesCount: Int, buttonGroup: ButtonGroup, index: Int, currentPage: Int) {
        if (ellipsesCount > 1) {
            add(createEllipsesComponent())
        } else {
            addPageButtonRange(buttonGroup, index, index + 1, currentPage)
        }
    }

    /**
     * Gets the number of omitted components
     * If there are 10 pages, the current page is page 5, and a maximum of 5 buttons are displayed,
     * then go to the end of the 2 buttons, and then remove the two omitted,
     * there should be 1 button in the middle, 4 omitted on the left and 3 omitted on the right
     */
    private fun getEllipsesComponentCount(pageCount: Int, currentPage: Int): Pair<Int, Int> {
        val leftEllipsesCount = (currentPage - 0 + 1) - 2 - getMiddleButtonEachSide()
        val rightEllipsesCount = (pageCount - 1 - currentPage + 1) - 2 - getMiddleButtonEachSide()
        return Pair(leftEllipsesCount.coerceAtLeast(0), rightEllipsesCount.coerceAtLeast(0))
    }

    private fun getMiddleButtonEachSide(): Int {
        val buttonCountAtMiddle = DEFAULT_MAX_PAGE_BUTTON_COUNT - 2 - 2
        return (buttonCountAtMiddle - 1) / 2
    }

    private fun createEllipsesComponent(): Component {
        return JLabel(ELLIPSES, SwingConstants.CENTER)
    }

    private fun addPageButtonRange(buttonGroup: ButtonGroup, start: Int, end: Int, currentPage: Int) {
        (start until end).forEach { addPageButton(buttonGroup, it, currentPage) }
    }

    private fun addPageButton(buttonGroup: ButtonGroup, pageNumber: Int, currentPage: Int) {
        val toggleButton = JToggleButton((pageNumber).toString())
        toggleButton.margin = Insets(1, 4, 1, 4)
        buttonGroup.add(toggleButton)
        add(toggleButton)
        if (pageNumber == currentPage) {
            toggleButton.isSelected = true
        }
        toggleButton.addActionListener { ae: ActionEvent ->
            gotoPage(ae.actionCommand.toInt())
        }
    }

    companion object {
        private const val DEFAULT_MAX_PAGE_BUTTON_COUNT = 9
        private const val ELLIPSES = "..."
    }
}