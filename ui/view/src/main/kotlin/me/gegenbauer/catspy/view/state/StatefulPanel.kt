package me.gegenbauer.catspy.view.state

import me.gegenbauer.catspy.strings.STRINGS
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingUtilities
import javax.swing.TransferHandler

class StatefulPanel : JPanel() {
    var listState: ListState = ListState.NONE
        set(value) {
            if (value == field) return
            field = value
            setStateInternal(value)
        }

    private val rootLayout = CardLayout()
    private val emptyContainerLayout = CardLayout()
    private var content: JComponent = JPanel()
    private var emptyContent: JComponent = JPanel()

    private val loadingProgress = JProgressBar().apply {
        preferredSize = Dimension(200, 20)
        string = STRINGS.ui.loading
        isStringPainted = true
    }
    private val container = JPanel().apply {
        layout = emptyContainerLayout
    }
    private val emptyContentContainer = JPanel().apply {
        layout = BorderLayout()
    }
    private val loadingProgressContainer = JPanel().apply {
        layout = GridBagLayout()
        add(loadingProgress)
    }

    init {
        layout = rootLayout

        add(content, "content")
        add(container, "empty")

        container.add(loadingProgressContainer, "loadingProgress")
        container.add(emptyContentContainer, "emptyImage")

        listState = ListState.EMPTY
    }

    fun hideEmptyContent() {
        emptyContent.isVisible = false
    }

    fun setEmptyContent(content: JComponent) {
        emptyContentContainer.removeAll()
        emptyContentContainer.add(content, BorderLayout.CENTER)
        emptyContent = content
        emptyContentContainer.revalidate()
        emptyContentContainer.repaint()
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
                container.isVisible = false
                content.isVisible = false
            }
        }

        if (listState == ListState.LOADING) {
            emptyContainerLayout.show(container, "loadingProgress")
        } else if (listState == ListState.EMPTY) {
            emptyContainerLayout.show(container, "emptyImage")
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
}

enum class ListState {
    NORMAL, EMPTY, LOADING, NONE
}