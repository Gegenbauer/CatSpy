package me.gegenbauer.catspy.common.ui.state

import me.gegenbauer.catspy.common.support.EmptyStatePanelTheme
import me.gegenbauer.catspy.common.ui.button.IconBarButton
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.log.GLog
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.GridBagLayout
import javax.swing.*

class StatefulPanel : JPanel() {
    var action: (JComponent) -> Unit = { _ -> }

    var state: State = State.NONE
        set(value) {
            if (value == field) return
            GLog.d(TAG, "[state] set $value")
            field = value
            setStateInternal(value)
        }

    private val rootLayout = CardLayout()
    private val emptyContainerLayout = CardLayout()
    private var content: JComponent = JPanel()

    private val emptyImage = IconBarButton(GIcons.State.Empty.get(60, 60)).apply {
        preferredSize = Dimension(120, 120)
        background = EmptyStatePanelTheme.iconBackground
        isBorderPainted = false
    }
    private val loadingProgress = JProgressBar().apply {
        preferredSize = Dimension(200, 20)
        string = "加载中"
        isStringPainted = true
    }
    private val emptyContainer = JPanel().apply {
        layout = emptyContainerLayout
    }
    private val emptyImageContainer = JPanel().apply {
        layout = GridBagLayout()
        add(emptyImage)
    }
    private val loadingProgressContainer = JPanel().apply {
        layout = GridBagLayout()
        add(loadingProgress)
    }

    init {
        layout = rootLayout

        emptyImage.addActionListener { action(it.source as JComponent) }

        add(content, "content")
        add(emptyContainer, "empty")

        emptyContainer.add(loadingProgressContainer, "loadingProgress")
        emptyContainer.add(emptyImageContainer, "emptyImage")

        state = State.EMPTY
    }

    private fun setStateInternal(state: State) {
        when (state) {
            State.NORMAL -> {
                setContentVisible(true)
            }

            State.EMPTY -> {
                setContentVisible(false)
            }

            State.LOADING -> {
                setContentVisible(false)
            }

            State.NONE -> {
                emptyContainer.isVisible = false
                content.isVisible = false
            }
        }

        if (state == State.LOADING) {
            emptyContainerLayout.show(emptyContainer, "loadingProgress")
        } else if (state == State.EMPTY) {
            emptyContainerLayout.show(emptyContainer, "emptyImage")
        }
        loadingProgress.isIndeterminate = state == State.LOADING

        SwingUtilities.updateComponentTreeUI(this)
    }

    private fun setContentVisible(visible: Boolean) {
        if (visible) {
            rootLayout.show(this, "content")
        } else {
            rootLayout.show(this, "empty")
        }
    }

    fun setContent(content: JComponent) {
        remove(this.content)
        add(content, "content")
        this.content = content
        content.transferHandler?.let {
            transferHandler = object : TransferHandler() {
                override fun canImport(support: TransferSupport?): Boolean {
                    return it.canImport(support)
                }

                override fun importData(support: TransferSupport?): Boolean {
                    return it.importData(support)
                }
            }
        }
    }

    enum class State {
        NORMAL, EMPTY, LOADING, NONE
    }

    companion object {
        private const val TAG = "EmptyStatePanel"
    }
}