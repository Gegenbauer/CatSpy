package me.gegenbauer.catspy.log.ui.panel

import com.github.weisj.darklaf.iconset.AllIcons
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.log.binding.LogMainBinding
import me.gegenbauer.catspy.log.ui.table.FilteredLogTableModel
import me.gegenbauer.catspy.log.ui.table.LogTableModel
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.*
import me.gegenbauer.catspy.view.button.ColorToggleButton
import me.gegenbauer.catspy.view.button.IconBarButton
import me.gegenbauer.catspy.view.combobox.filterComboBox
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.JLabel
import javax.swing.JPanel

internal class SearchPanel(
    logTableModel: LogTableModel,
    override val contexts: Contexts = Contexts.default
) : JPanel(), Context {
    val closeBtn = IconBarButton(AllIcons.Navigation.Close.get()) applyTooltip STRINGS.toolTip.searchCloseBtn
    val searchCombo = filterComboBox() applyTooltip STRINGS.toolTip.searchCombo
    val searchMatchCaseToggle = ColorToggleButton("Aa") applyTooltip STRINGS.toolTip.searchCaseToggle
    var currentLogTableModel: LogTableModel = logTableModel
        set(value) {
            updateTargetLabel(value)
            field = value
        }

    private var targetLabel = JLabel("${STRINGS.ui.filter} ${STRINGS.ui.log}") applyTooltip STRINGS.toolTip.searchTargetLabel
    private val upBtn = IconBarButton(GIcons.Action.Up.get()) applyTooltip STRINGS.toolTip.searchPrevBtn
    private val downBtn = IconBarButton(GIcons.Action.Down.get()) applyTooltip STRINGS.toolTip.searchNextBtn
    private val contentPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 2))
    private val statusPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 5, 2))

    private val searchActionHandler = SearchActionHandler()
    private val searchKeyHandler = SearchKeyHandler()

    init {
        configureUI()
        registerEvent()
    }

    private fun configureUI() {
        searchCombo.tooltipEnabled = false
        searchCombo.isEditable = true
        searchCombo.setWidth(400)

        contentPanel.add(searchCombo)
        contentPanel.add(searchMatchCaseToggle)
        contentPanel.add(upBtn)
        contentPanel.add(downBtn)

        statusPanel.add(targetLabel)
        statusPanel.add(closeBtn)

        layout = BorderLayout()
        add(contentPanel, BorderLayout.WEST)
        add(statusPanel, BorderLayout.EAST)
    }

    private fun registerEvent() {
        registerComboBoxEditorEvent()
        upBtn.addActionListener(searchActionHandler)
        downBtn.addActionListener(searchActionHandler)
        closeBtn.addActionListener(searchActionHandler)

        registerStrokeWhenFocused(Key.ENTER, "Move to next search result") { moveToNext() }
        registerStrokeWhenFocused(Key.S_ENTER, "Move to previous search result") { moveToPrev() }
    }

    fun registerComboBoxEditorEvent() {
        searchCombo.keyListener = searchKeyHandler
    }

    override fun setVisible(visible: Boolean) {
        super.setVisible(visible)

        if (visible) {
            searchCombo.requestFocus()
            searchCombo.editor.selectAll()
        } else {
            searchCombo.editorComponent.text = ""
            searchCombo.hidePopup()
        }
    }

    private fun updateTargetLabel(logTableModel: LogTableModel) {
        targetLabel.text = if (logTableModel is FilteredLogTableModel) {
            "${STRINGS.ui.filter} ${STRINGS.ui.log}"
        } else {
            "${STRINGS.ui.full} ${STRINGS.ui.log}"
        }
    }

    fun moveToNext() {
        currentLogTableModel.moveToNextSearchResult()
    }

    fun moveToPrev() {
        currentLogTableModel.moveToPreviousSearchResult()
    }

    private inner class SearchActionHandler : ActionListener {
        override fun actionPerformed(event: ActionEvent) {
            when (event.source) {
                upBtn -> {
                    moveToPrev()
                }

                downBtn -> {
                    moveToNext()
                }

                closeBtn -> {
                    val logMainPanel = contexts.getContext(BaseLogMainPanel::class.java)
                    logMainPanel ?: return
                    ServiceManager.getContextService(logMainPanel, LogMainBinding::class.java)
                        .searchPanelVisible.updateValue(false)
                }
            }
        }
    }

    private inner class SearchKeyHandler : KeyAdapter() {
        override fun keyReleased(event: KeyEvent) {
            if (event.keyEventInfo == Key.ENTER.released() || event.keyEventInfo == Key.S_ENTER.released()) {
                this@SearchPanel::moveToPrev
                    .takeIf { KeyEvent.SHIFT_DOWN_MASK == event.modifiersEx }
                    ?.invoke() ?: this@SearchPanel::moveToNext.invoke()
            }
        }
    }
}
