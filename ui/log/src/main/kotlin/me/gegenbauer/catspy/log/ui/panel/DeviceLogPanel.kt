package me.gegenbauer.catspy.log.ui.panel

import com.malinskiy.adam.request.device.Device
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceManager
import me.gegenbauer.catspy.ddmlib.device.DeviceListListener
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.log.binding.LogMainBinding
import me.gegenbauer.catspy.log.ui.table.FilteredLogTableModel
import me.gegenbauer.catspy.log.ui.table.LogTableModel
import me.gegenbauer.catspy.strings.GlobalStrings
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.button.ColorToggleButton
import me.gegenbauer.catspy.view.combobox.filterComboBox
import me.gegenbauer.catspy.view.combobox.toHistoryItemList
import me.gegenbauer.catspy.view.filter.FilterItem
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.EMPTY_ITEM
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.rebuild
import javax.swing.Icon

class DeviceLogPanel: BaseLogPanel() {

    override val tag: String = "DeviceLogPanel"

    override val tabName: String = STRINGS.ui.tabDeviceLog
    override val tabIcon: Icon = GIcons.Tab.DeviceLog.get()

    override val showProcessToggle = ColorToggleButton(GlobalStrings.PACKAGE, STRINGS.toolTip.packageToggle)
    override val showProcessCombo = filterComboBox(tooltip = STRINGS.toolTip.packageToggle)

    override val currentPidFilter: FilterItem = EMPTY_ITEM
    override val currentPackageFilter: FilterItem
        get() = if (logMainBinding.packageFilterEnabled.getValueNonNull()) {
            showProcessCombo.filterItem.rebuild(logMainBinding.filterMatchCaseEnabled.getValueNonNull())
        } else {
            EMPTY_ITEM
        }

    override val processComboWidthWeight: Double = 0.2

    override val fullTableModel = LogTableModel(logViewModel, true)
    override val filteredTableModel = FilteredLogTableModel(logViewModel, true)

    override fun bindProcessComponents(mainBinding: LogMainBinding) {
        mainBinding.apply {
            bindLogFilter(
                showProcessCombo,
                showProcessToggle,
                packageFilterSelectedIndex,
                packageFilterHistory,
                packageFilterEnabled,
                packageFilterCurrentContent,
                packageFilterErrorMessage
            )
        }
    }

    override fun createUI() {
        super.createUI()
        splitLogWithStatefulPanel.hideEmptyImage()
    }

    override fun registerEvent() {
        super.registerEvent()
        scope.launch {
            delay(200)
            ServiceManager.getContextService(AdamDeviceManager::class.java).registerDevicesListener(devicesChangeListener)
        }
    }

    private val devicesChangeListener = DeviceListListener {
        refreshDevices(it)
    }

    private fun refreshDevices(devices: List<Device>) {
        val currentDevice = logViewModel.device
        val newDeviceList = devices.map { it.serial }.sortedBy { if (it == currentDevice) -1 else 1 }
        logMainBinding.connectedDevices.updateValue(newDeviceList.toHistoryItemList())
        startBtn.isEnabled = devices.isEmpty().not()
    }
}