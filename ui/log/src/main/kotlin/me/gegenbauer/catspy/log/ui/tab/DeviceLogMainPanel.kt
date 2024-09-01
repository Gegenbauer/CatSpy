package me.gegenbauer.catspy.log.ui.tab

import com.malinskiy.adam.request.device.Device
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceMonitor
import me.gegenbauer.catspy.ddmlib.device.AdbServerStatusListener
import me.gegenbauer.catspy.ddmlib.device.DeviceListObserver
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.java.ext.Bundle
import me.gegenbauer.catspy.concurrency.GlobalEventManager
import me.gegenbauer.catspy.concurrency.OpenAdbPathSettingsEvent
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.metadata.LogMetadataChangeListener
import me.gegenbauer.catspy.log.metadata.LogMetadataManager
import me.gegenbauer.catspy.log.serialize.LogMetadataModel
import me.gegenbauer.catspy.log.serialize.toLogMetadata
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.panel.StatusBar

class DeviceLogMainPanel: BaseLogMainPanel(), LogMetadataChangeListener {
    override val tag: String = "DeviceLogMainPanel"

    private val idleStatus: String = "${STRINGS.ui.adb} ${STRINGS.ui.stop}"

    private val logMetadataManager: LogMetadataManager
        get() = ServiceManager.getContextService(LogMetadataManager::class.java)

    override fun onSetup(bundle: Bundle?) {
        super.onSetup(bundle)

        checkAdbPath()
        adbServerStatusWarningBtn.addActionListener {
            showAdbPathSettings()
        }

        val deviceManager = ServiceManager.getContextService(AdamDeviceMonitor::class.java)
        deviceManager.tryStartMonitor()

        logMetadataManager.addOnMetadataChangeListener(this)
    }

    override fun createUI() {
        super.createUI()
        splitLogWithStatefulPanel.hideEmptyImage()
        deviceCombo.isVisible = true
        saveBtn.isVisible = true
    }

    private fun checkAdbPath() {
        val adbPath = SettingsManager.adbPath
        if (adbPath.isEmpty()) {
            showAdbPathSettings()
        }
    }

    private fun showAdbPathSettings() {
        GlobalEventManager.publish(OpenAdbPathSettingsEvent)
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

    override fun onStartClicked() {
        super.onStartClicked()
        startLogcat()
    }

    private fun startLogcat() {
        stopAll()
        if (logMainBinding.connectedDevices.value.isNullOrEmpty()) return

        logViewModel.startProduceDeviceLog(logMainBinding.currentDevice.value ?: EMPTY_STRING)

        GLog.d(tag, "[startLogcat] device: ${logMainBinding.currentDevice.value ?: EMPTY_STRING}")

        updateLogFilter()
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
        logMainBinding.connectedDevices.updateValue(newDeviceList)
        if (currentDevice.isEmpty() || newDeviceList.contains(currentDevice).not()) {
            logMainBinding.currentDevice.updateValue(newDeviceList.firstOrNull())
        }
        startBtn.isEnabled = devices.isEmpty().not()
    }

    override fun onMetadataChanged(old: LogMetadataModel, new: LogMetadataModel) {
        if (old.logType == new.logType && new.logType == logConf.logMetaData.logType) {
            logConf.setLogMetadata(new.toLogMetadata())
        }
    }

    override fun afterTaskStateChanged(state: TaskUIState) {
        super.afterTaskStateChanged(state)
        when (state) {
            is TaskStarted -> {
                logStatus =
                    StatusBar.LogStatusRunning(STRINGS.ui.adb, logViewModel.tempLogFile.absolutePath ?: EMPTY_STRING)
            }

            is TaskIdle -> {
                if (isLogTableEmpty) {
                    logStatus = StatusBar.LogStatusIdle(idleStatus)
                } else {
                    logStatus = StatusBar.LogStatusIdle(idleStatus, logViewModel.tempLogFile.absolutePath ?: EMPTY_STRING)
                }
            }
        }
    }

    override fun afterLogStatusChanged(status: StatusBar.LogStatus) {
        super.afterLogStatusChanged(status)
        setTabName(logMainBinding.currentDevice.value ?: EMPTY_STRING)
    }

    override fun clearAllLogs() {
        super.clearAllLogs()
        if (taskState is TaskIdle) {
            logStatus = StatusBar.LogStatusIdle(idleStatus)
        }
    }

    override fun destroy() {
        super.destroy()
        val deviceManager = ServiceManager.getContextService(AdamDeviceMonitor::class.java)
        deviceManager.tryStopMonitor()
        deviceManager.unregisterDevicesListener(devicesChangeObserver)
        deviceManager.unregisterAdbServerStatusListener(adbStatusChangeObserver)
    }
}