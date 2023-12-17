package me.gegenbauer.catspy.view.state

import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.button.IconBarButton
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.GridBagLayout
import javax.swing.*

class StatefulPanel : JPanel() {
    var action: (JComponent) -> Unit = { _ -> }

    var listState: ListState = ListState.NONE
        set(value) {
            if (value == field) return
            field = value
            setStateInternal(value)
        }

    private val rootLayout = CardLayout()
    private val emptyContainerLayout = CardLayout()
    private var content: JComponent = JPanel()

    private val emptyImage = IconBarButton(GIcons.State.Empty.get(60, 60)).apply {
        preferredSize = Dimension(120, 120)
        isBorderPainted = false
    }
    private val loadingProgress = JProgressBar().apply {
        preferredSize = Dimension(200, 20)
        string = STRINGS.ui.loading
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

        listState = ListState.EMPTY
    }

    private fun setStateInternal(listState: ListState) {
        when (listState) {
            ListState.NORMAL -> {
                setContentVisible(true)
            }

            ListState.EMPTY -> {
                setContentVisible(false)
            }

            ListState.LOADING -> {
                setContentVisible(false)
            }

            ListState.NONE -> {
                emptyContainer.isVisible = false
                content.isVisible = false
            }
        }

        if (listState == ListState.LOADING) {
            emptyContainerLayout.show(emptyContainer, "loadingProgress")
        } else if (listState == ListState.EMPTY) {
            emptyContainerLayout.show(emptyContainer, "emptyImage")
        }
        loadingProgress.isIndeterminate = listState == ListState.LOADING

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

    companion object {
        private const val TAG = "StatefulPanel"
    }
}

enum class ListState {
    NORMAL, EMPTY, LOADING, NONE
}