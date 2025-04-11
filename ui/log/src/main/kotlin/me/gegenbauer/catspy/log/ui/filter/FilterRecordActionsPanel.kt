package me.gegenbauer.catspy.log.ui.filter

import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.filter.FilterRecord
import me.gegenbauer.catspy.log.ui.customize.CenteredDualDirectionPanel
import me.gegenbauer.catspy.log.ui.customize.ParamEditor
import me.gegenbauer.catspy.log.ui.customize.ParamVerifier
import me.gegenbauer.catspy.log.ui.filter.FavoriteFilterPanel.Companion.KEY_FILTER_RECORDS
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.strings.get
import me.gegenbauer.catspy.utils.persistence.Preferences
import me.gegenbauer.catspy.utils.ui.Key
import me.gegenbauer.catspy.utils.ui.findFrameFromParent
import me.gegenbauer.catspy.utils.ui.installKeyStrokeEscClosing
import me.gegenbauer.catspy.utils.ui.keyEventInfo
import me.gegenbauer.catspy.utils.ui.showInfoDialog
import me.gegenbauer.catspy.utils.ui.showWarningDialog
import me.gegenbauer.catspy.view.button.GButton
import me.gegenbauer.catspy.view.button.IconBarButton
import me.gegenbauer.catspy.view.panel.HorizontalFlexibleHeightLayout
import java.awt.BorderLayout
import java.awt.Window
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JPanel

class FilterRecordActionsPanel : JPanel(), OnAddRequestListener {

    private var currentFilterRecordProvider: FilterRecordProvider? = null

    private val saveFilterButton = IconBarButton(GIcons.Action.SaveFilter.get()).apply {
        toolTipText = STRINGS.toolTip.saveFilter
    }
    private val deleteAllRecords = IconBarButton(GIcons.Action.DeleteAllFilters.get()).apply {
        toolTipText = STRINGS.toolTip.deleteAllRecords
    }
    private var onAddRequestListener: OnAddRequestListener? = null

    init {
        layout = HorizontalFlexibleHeightLayout()
        border = BorderFactory.createEmptyBorder(0, 6, 0, 10)
        add(saveFilterButton)
        add(deleteAllRecords)

        saveFilterButton.addActionListener {
            val filterName = showNameEditDialog()
            if (filterName.isNotEmpty()) {
                onAddRequest(filterName)
            }
        }
        deleteAllRecords.addActionListener {
            val records = Preferences.get<List<FilterRecord>>(KEY_FILTER_RECORDS, emptyList())
            if (records.isEmpty()) {
                return@addActionListener
            }
            val actions = listOf(
                STRINGS.ui.ok to { true },
                STRINGS.ui.cancel to { false }
            )
            val res = showWarningDialog(
                findFrameFromParent(), EMPTY_STRING,
                STRINGS.ui.clearFilterRecordsWarningContent, actions, 1
            )
            if (res) {
                Preferences.remove(KEY_FILTER_RECORDS)
            }
        }
    }

    fun setOnAddRequestListener(listener: OnAddRequestListener) {
        onAddRequestListener = listener
    }

    fun setCurrentFilterRecordProvider(provider: FilterRecordProvider) {
        currentFilterRecordProvider = provider
    }

    override fun onAddRequest(filterName: String) {
        onAddRequestListener?.onAddRequest(filterName)
    }

    private fun showNameEditDialog(): String {
        val parent = findFrameFromParent<JFrame>()
        val currentFilterRecord = currentFilterRecordProvider?.getFilterRecord(EMPTY_STRING) ?: return EMPTY_STRING
        if (currentFilterRecord.isEmpty()) {
            showInfoDialog(parent, EMPTY_STRING, STRINGS.ui.saveEmptyFilterWarning)
            return EMPTY_STRING
        }
        val records = Preferences.get<List<FilterRecord>>(KEY_FILTER_RECORDS, emptyList())
        val equivalentRecord = records.find { it.equalsIgnoreName(currentFilterRecord) }
        if (equivalentRecord != null) {
            val actions = listOf(
                STRINGS.ui.ok to { true },
                STRINGS.ui.cancel to { false }
            )
            if (!showWarningDialog(parent, EMPTY_STRING, STRINGS.ui.saveDuplicationFilterRecordWarning, actions)) {
                return EMPTY_STRING
            }
        }
        val dialog = NameEditDialog(records, parent, equivalentRecord?.name ?: EMPTY_STRING)
        dialog.isVisible = true
        return dialog.getNewName()
    }

    private class NameEditDialog(
        private val records: List<FilterRecord>,
        private val parent: Window,
        oldName: String = EMPTY_STRING,
    ) : JDialog(parent) {
        private val nameTextField = ParamEditor(STRINGS.toolTip.filterRecordName)
        private val okButton = GButton(STRINGS.ui.ok)
        private val cancelButton = GButton(STRINGS.ui.cancel)
        private val buttonsPanel = CenteredDualDirectionPanel()

        private val verifier = ParamVerifier {
            val filterName = nameTextField.text
            if (filterName.isEmpty()) {
                return@ParamVerifier ParamVerifier.Result.Invalid(STRINGS.toolTip.contentBlankWarning)
            }
            return@ParamVerifier ParamVerifier.Result.Valid
        }

        init {
            modalityType = ModalityType.APPLICATION_MODAL
            title = STRINGS.ui.saveFilterTitle
            setSize(300, 100)
            defaultCloseOperation = DISPOSE_ON_CLOSE
            installKeyStrokeEscClosing(this)

            getRootPane().defaultButton = okButton
            setLocationRelativeTo(parent)
            contentPane.layout = BorderLayout()
            nameTextField.startEditing()
            nameTextField.requestFocus()
            nameTextField.setVerifier(verifier)
            nameTextField.text = oldName
            contentPane.add(nameTextField, BorderLayout.CENTER)
            contentPane.add(buttonsPanel, BorderLayout.SOUTH)
            buttonsPanel.addRight(cancelButton)
            buttonsPanel.addRight(okButton)
            okButton.addActionListener {
                if (!nameTextField.isEditValid()) {
                    return@addActionListener
                }
                val isNameExist = records.any { it.name == nameTextField.text }
                if (isNameExist && !showNameExistWarning()) {
                    nameTextField.text = EMPTY_STRING
                }
                isVisible = false
            }

            cancelButton.addActionListener {
                isVisible = false
            }

            nameTextField.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    if (e.keyEventInfo == Key.ENTER) {
                        okButton.doClick()
                    }
                }
            })
            nameTextField.moveCaretPosition(nameTextField.text.length)
        }

        fun getNewName(): String {
            return nameTextField.text.takeIf { nameTextField.isEditValid() } ?: EMPTY_STRING
        }

        private fun showNameExistWarning(): Boolean {
            val actions = listOf(
                STRINGS.ui.ok to { true },
                STRINGS.ui.cancel to { false }
            )
            return showWarningDialog(
                parent,
                EMPTY_STRING,
                STRINGS.ui.filterRecordNameAlreadyExistWarning,
                actions
            )
        }

        companion object {
            private const val MAX_NAME_LENGTH = 20
        }
    }
}

fun interface OnAddRequestListener {
    fun onAddRequest(filterName: String)
}