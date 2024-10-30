package me.gegenbauer.catspy.view.state

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.button.CloseButton
import java.awt.BorderLayout
import java.awt.CardLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JProgressBar
import javax.swing.SwingUtilities
import javax.swing.TransferHandler

class StatefulPanel(override val contexts: Contexts = Contexts.default) : JPanel(), Context {
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
        preferredSize = Dimension(300, 20)
        string = STRINGS.ui.loading
        isStringPainted = true
    }
    private val container = JPanel().apply {
        layout = emptyContainerLayout
    }
    private val emptyContentContainer = JPanel().apply {
        layout = BorderLayout()
    }
    private val cancelButton = CloseButton().apply {
        addActionListener {

        }
    }
    private val loadingContentContainer = JPanel().apply {
        layout = GridBagLayout()
        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            weightx = 0.0
            weighty = 0.0
            fill = GridBagConstraints.NONE
        }
        add(loadingProgress, gbc)

        gbc.apply {
            gridx = 1
            weightx = 0.0
            fill = GridBagConstraints.NONE
        }
        add(cancelButton, gbc)
    }

    init {
        layout = rootLayout

        add(content, "content")
        add(container, "empty")

        container.add(loadingContentContainer, "loadingProgress")
        container.add(emptyContentContainer, "emptyImage")

        listState = ListState.Empty
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
            ListState.Normal -> {
                setContentVisible(true)
            }

            ListState.Empty -> {
                setContentVisible(false)
            }

            is ListState.Loading -> {
                setContentVisible(false)
            }

            ListState.NONE -> {
                container.isVisible = false
                content.isVisible = false
            }
        }

        if (listState is ListState.Loading) {
            loadingProgress.isIndeterminate = listState.isIntermediate
            cancelButton.isVisible = !listState.isIntermediate
            loadingProgress.value = listState.progress
            loadingProgress.maximum = listState.max
            emptyContainerLayout.show(container, "loadingProgress")
        } else if (listState == ListState.Empty) {
            emptyContainerLayout.show(container, "emptyImage")
        }

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

sealed class ListState {
    object Normal : ListState()
    object Empty : ListState()
    class Loading(val isIntermediate: Boolean = false, val progress: Int = 0, val max: Int = 100) : ListState()
    object NONE : ListState()

    companion object {
        fun loading(progress: Int, max: Int): ListState {
            return Loading(isIntermediate = false, progress = progress, max = max)
        }

        fun intermediateLoading(): ListState {
            return Loading(isIntermediate = true)
        }
    }
}