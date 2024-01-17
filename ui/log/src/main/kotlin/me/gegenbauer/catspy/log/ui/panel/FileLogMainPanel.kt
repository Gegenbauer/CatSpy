package me.gegenbauer.catspy.log.ui.panel

import me.gegenbauer.catspy.log.binding.LogMainBinding
import me.gegenbauer.catspy.configuration.GlobalStrings
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.button.ColorToggleButton
import me.gegenbauer.catspy.view.combobox.filterComboBox
import me.gegenbauer.catspy.view.filter.FilterItem
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.rebuild

class FileLogMainPanel: BaseLogMainPanel() {
    override val tag: String = "FileLogMainPanel"
    override val processFilterToggle = ColorToggleButton(GlobalStrings.PID, STRINGS.toolTip.pidToggle)
    override val processFilterCombo = filterComboBox(tooltip = STRINGS.toolTip.pidToggle)

    override val currentPidFilter: FilterItem
        get() = if (logMainBinding.pidFilterEnabled.getValueNonNull()) {
            processFilterCombo.filterItem.rebuild(logMainBinding.filterMatchCaseEnabled.getValueNonNull())
        } else {
            FilterItem.EMPTY_ITEM
        }
    override val currentPackageFilter: FilterItem = FilterItem.EMPTY_ITEM

    override fun bindProcessComponents(mainBinding: LogMainBinding) {
        mainBinding.apply {
            bindLogFilter(
                processFilterCombo,
                processFilterToggle,
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