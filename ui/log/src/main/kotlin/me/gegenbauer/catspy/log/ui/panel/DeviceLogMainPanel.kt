package me.gegenbauer.catspy.log.ui.panel

import com.malinskiy.adam.request.device.Device
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceMonitor
import me.gegenbauer.catspy.ddmlib.device.DeviceListObserver
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.log.binding.LogMainBinding
import me.gegenbauer.catspy.log.ui.table.FilteredLogTableModel
import me.gegenbauer.catspy.log.ui.table.LogTableModel
import me.gegenbauer.catspy.configuration.GlobalStrings
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.ddmlib.device.AdbServerStatusListener
import me.gegenbauer.catspy.java.ext.OpenAdbPathSettingsEvent
import me.gegenbauer.catspy.java.ext.globalEventPublisher
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.button.ColorToggleButton
import me.gegenbauer.catspy.view.combobox.filterComboBox
import me.gegenbauer.catspy.view.combobox.toHistoryItemList
import me.gegenbauer.catspy.view.filter.FilterItem
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.EMPTY_ITEM
import me.gegenbauer.catspy.view.filter.FilterItem.Companion.rebuild
import javax.swing.Icon

class DeviceLogMainPanel: BaseLogMainPanel() {

    override val tag: String = "DeviceLogMainPanel"

    override val tabName: String = STRINGS.ui.tabDeviceLog
    override val tabIcon: Icon = GIcons.Tab.DeviceLog.get()

    override val processFilterToggle = ColorToggleButton(GlobalStrings.PACKAGE, STRINGS.toolTip.packageToggle)
    override val processFilterCombo = filterComboBox(tooltip = STRINGS.toolTip.packageToggle)

    override val currentPidFilter: FilterItem = EMPTY_ITEM
    override val currentPackageFilter: FilterItem
        get() = if (logMainBinding.packageFilterEnabled.getValueNonNull()) {
            processFilterCombo.filterItem.rebuild(logMainBinding.filterMatchCaseEnabled.getValueNonNull())
        } else {
            EMPTY_ITEM
        }

    override val processComboWidthWeight: Double = 0.2

    override val fullTableModel = LogTableModel(logViewModel, true)
    override val filteredTableModel = FilteredLogTableModel(logViewModel, true)

    override fun bindProcessComponents(mainBinding: LogMainBinding) {
        mainBinding.apply {
            bindLogFilter(
                processFilterCombo,
                processFilterToggle,
                packageFilterSelectedIndex,
                packageFilterHistory,
                packageFilterEnabled,
                packageFilterCurrentContent,
                packageFilterErrorMessage
            )
        }
    }

    override fun setup() {
        super.setup()
        val deviceManager = ServiceManager.getContextService(AdamDeviceMonitor::class.java)
        deviceManager.tryStartMonitor()
    }

    override fun onVisible() {
        super.onVisible()
        checkAdbPath()
        adbServerStatusWarningBtn.addActionListener {
            showAdbPathSettings()
        }
    }

    override fun createUI() {
        super.createUI()
        splitLogWithStatefulPanel.hideEmptyImage()
    }

    private fun checkAdbPath() {
        val adbPath = SettingsManager.adbPath
        if (adbPath.isEmpty()) {
            showAdbPathSettings()
        }
    }

    private fun showAdbPathSettings() {
        globalEventPublisher.publish(OpenAdbPathSettingsEvent)
    }

    override fun registerEvent() {
        super.registerEvent()
        scope.launch {
            delay(200)
            ServiceManager.getContextService(AdamDeviceMonitor::class.java).apply {
                registerDevicesListener(devicesChangeObserver)
                registerAdbServerStatusListener(adbStatusChangeObserver)
            }
        }
    }

    private val devicesChangeObserver = DeviceListObserver {
        refreshDevices(it)
    }

    private val adbStatusChangeObserver = AdbServerStatusListener {
        logMainBinding.adbServerStatusWarningVisibility.updateValue(it.not())
    }

    private fun refreshDevices(devices: List<Device>) {
        val currentDevice = logViewModel.device
        val newDeviceList = devices.map { it.serial }.sortedBy { if (it == currentDevice) -1 else 1 }
        logMainBinding.connectedDevices.updateValue(newDeviceList.toHistoryItemList())
        startBtn.isEnabled = devices.isEmpty().not()
    }

    override fun destroy() {
        super.destroy()
        val deviceManager = ServiceManager.getContextService(AdamDeviceMonitor::class.java)
        deviceManager.tryStopMonitor()
        deviceManager.unregisterDevicesListener(devicesChangeObserver)
        deviceManager.unregisterAdbServerStatusListener(adbStatusChangeObserver)
    }
}