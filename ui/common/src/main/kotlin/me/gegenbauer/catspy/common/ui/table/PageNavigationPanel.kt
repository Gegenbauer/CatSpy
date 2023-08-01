package me.gegenbauer.catspy.common.ui.table

import java.awt.Component
import java.awt.Dimension
import java.awt.Insets
import java.awt.event.ActionEvent
import javax.swing.*

class PageNavigationPanel<T>(
   private val pageable: Pageable<T>
) : JPanel(), Pageable<T> by pageable {

    init {
        minimumSize = Dimension(0, 0)
        refreshPageNavigationButtons()
        pageable.observablePageMetaData.addObserver { pageMetaData ->
            pageMetaData ?: return@addObserver
            refreshPageNavigationButtons(pageMetaData.pageCount, pageMetaData.currentPage)
        }
    }

    /**
     * Given MAX_PAGE_BUTTON_COUNT = 5, pageCount = 10
     * 0, 1, 2, 3, 4, 5, 6, 7, 8, 9
     */
    private fun refreshPageNavigationButtons(pageCount: Int = 0, currentPage: Int = 0) {
        removeAll()

        if (pageCount == 0) return

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
        } else  {
            addPageButtonRange(buttonGroup, index, index + 1, currentPage)
        }
    }

    /**
     * 获取省略的组件个数
     * 假如有10页，当前页是第5页，最多显示5个按钮，那么去掉头尾2个按钮，再去掉省略的两个，中间应该是1个按钮，左边省略 4 个，右边省略 3 个
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
        toggleButton.setMargin(Insets(1, 4, 1, 4))
        buttonGroup.add(toggleButton)
        add(toggleButton)
        if (pageNumber == currentPage) {
            toggleButton.setSelected(true)
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