package me.gegenbauer.catspy.log.ui.panel

import me.gegenbauer.catspy.databinding.bind.withName
import me.gegenbauer.catspy.log.binding.LogMainBinding
import me.gegenbauer.catspy.strings.GlobalStrings
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.button.ColorToggleButton
import me.gegenbauer.catspy.view.combobox.filterComboBox
import me.gegenbauer.catspy.view.filter.FilterItem
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.rebuild

class FileLogPanel: BaseLogPanel() {
    override val tag: String = "FileLogPanel"
    override val showProcessToggle = ColorToggleButton(GlobalStrings.PID, STRINGS.toolTip.pidToggle)
    override val showProcessCombo = filterComboBox(tooltip = STRINGS.toolTip.pidToggle) withName GlobalStrings.PID

    override val currentPidFilter: FilterItem
        get() = if (logMainBinding.pidFilterEnabled.getValueNonNull()) {
            showProcessCombo.filterItem.rebuild(logMainBinding.filterMatchCaseEnabled.getValueNonNull())
        } else {
            FilterItem.EMPTY_ITEM
        }
    override val currentPackageFilter: FilterItem = FilterItem.EMPTY_ITEM

    override fun bindProcessComponents(mainBinding: LogMainBinding) {
        mainBinding.apply {
            bindLogFilter(
                showProcessCombo,
                showProcessToggle,
                pidFilterSelectedIndex,
                pidFilterHistory,
                pidFilterEnabled,
                pidFilterCurrentContent,
                pidFilterErrorMessage
            )
        }
    }

    override fun createUI() {
        super.createUI()
        deviceCombo.isVisible = false
        saveBtn.isVisible = false
    }

    override fun bindDeviceComponents(mainBinding: LogMainBinding) {
        // do nothing
    }

    override fun afterTaskStateChanged(state: TaskUIState) {
        if (state is TaskIdle) {
            startBtn.isEnabled = false
        }
    }
}