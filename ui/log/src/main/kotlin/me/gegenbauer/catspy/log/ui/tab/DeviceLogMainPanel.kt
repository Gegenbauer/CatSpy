package me.gegenbauer.catspy.log.ui.tab

import com.malinskiy.adam.request.device.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.GlobalEventManager
import me.gegenbauer.catspy.concurrency.IgnoreFastCallbackScheduler
import me.gegenbauer.catspy.concurrency.OpenAdbPathSettingsEvent
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.configuration.SettingsManager
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.bindDual
import me.gegenbauer.catspy.databinding.property.support.visibilityProperty
import me.gegenbauer.catspy.ddmlib.adb.AdbConf
import me.gegenbauer.catspy.ddmlib.adb.isServerRunning
import me.gegenbauer.catspy.ddmlib.adb.startServer
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceMonitor
import me.gegenbauer.catspy.ddmlib.device.AdbServerStatusListener
import me.gegenbauer.catspy.ddmlib.device.DeviceListObserver
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.java.ext.Bundle
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.log.binding.LogMainBinding
import me.gegenbauer.catspy.log.metadata.LogMetadataChangeListener
import me.gegenbauer.catspy.log.metadata.LogMetadataManager
import me.gegenbauer.catspy.log.serialize.LogMetadataModel
import me.gegenbauer.catspy.log.serialize.toLogMetadata
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.ui.Key
import me.gegenbauer.catspy.utils.ui.registerStroke
import me.gegenbauer.catspy.utils.ui.setWidth
import me.gegenbauer.catspy.view.button.IconBarButton
import me.gegenbauer.catspy.view.combobox.readOnlyComboBox
import me.gegenbauer.catspy.view.panel.StatusBar
import java.awt.Component
import java.io.File

class DeviceLogMainPanel : BaseLogMainPanel(), LogMetadataChangeListener {
    override val tag: String = "DeviceLogMainPanel"

    private val deviceCombo = readOnlyComboBox(STRINGS.toolTip.devicesCombo)
    private val adbServerStatusWarningBtn = IconBarButton(
        GIcons.Action.Warning.get(),
        STRINGS.ui.adbServerNotStartedWarn
    ).apply {
        isVisible = false
    }
    private val idleStatus: String = "${STRINGS.ui.adb} ${STRINGS.ui.stop}"
    private val adbServiceUpdater = IgnoreFastCallbackScheduler(Dispatchers.UI, 20)

    private val logMetadataManager: LogMetadataManager
        get() = ServiceManager.getContextService(LogMetadataManager::class.java)

    private val deviceManager: AdamDeviceMonitor
        get() = ServiceManager.getContextService(AdamDeviceMonitor::class.java)

    override fun onSetup(bundle: Bundle?) {
        super.onSetup(bundle)

        checkAdbPath()
        adbServerStatusWarningBtn.addActionListener {
            showAdbPathSettings()
        }

        deviceManager.tryStartMonitor()
        scope.launch {
            if (!isServerRunning()) {
                startServer(AdbConf(SettingsManager.adbPath))
            }
        }

        logMetadataManager.addOnMetadataChangeListener(this)
    }

    override fun createUI() {
        super.createUI()
        deviceCombo.setWidth(150)

        splitLogWithStatefulPanel.hideEmptyContent()
        deviceCombo.isVisible = true
        logConf.getLogBufferSelectPanel().isVisible = logConf.isPreviewMode.not()
    }

    override fun getCustomToolbarComponents(): List<Component> {
        return super.getCustomToolbarComponents() + adbServerStatusWarningBtn + deviceCombo
    }

    override fun bind(binding: LogMainBinding) {
        super.bind(binding)
        binding.apply {
            bindNormalCombo(deviceCombo, connectedDevices, currentDevice)
            visibilityProperty(adbServerStatusWarningBtn) bindDual adbServerStatusWarningVisibility
        }
    }

    private fun checkAdbPath(): Boolean {
        val adbPath = SettingsManager.adbPath
        if (adbPath.isEmpty() || File(adbPath).exists().not()) {
            showAdbPathSettings()
            return false
        }
        return true
    }

    private fun showAdbPathSettings() {
        GlobalEventManager.publish(OpenAdbPathSettingsEvent)
    }

    override fun registerEvent() {
        super.registerEvent()
        scope.launch {
            delay(200)
            deviceManager.registerDevicesListener(devicesChangeObserver)
            deviceManager.registerAdbServerStatusListener(adbStatusChangeObserver)
        }
    }

    override fun registerStrokes() {
        super.registerStrokes()
        registerStroke(Key.C_L, "Device Combo Request Focus") { deviceCombo.requestFocus() }
    }

    override fun onStartClicked() {
        super.onStartClicked()
        if (checkAdbPath()) {
            startLogcat()
        }
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
        adbServiceUpdater.schedule {
            logMainBinding.adbServerStatusWarningVisibility.updateValue(it.not())
        }
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
        deviceCombo.isEnabled = state is TaskIdle
        logConf.setLogBufferSelectorEnabled(state is TaskIdle)
        when (state) {
            is TaskStarted -> {
                logStatus = StatusBar.LogStatusRunning(
                    STRINGS.ui.adb,
                    logViewModel.tempLogFile.absolutePath ?: EMPTY_STRING
                )
            }

            is TaskIdle -> {
                logStatus = if (isLogTableEmpty) {
                    StatusBar.LogStatusIdle(idleStatus)
                } else {
                    StatusBar.LogStatusIdle(
                        idleStatus,
                        logViewModel.tempLogFile.absolutePath ?: EMPTY_STRING
                    )
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
        deviceManager.tryStopMonitor()
        deviceManager.unregisterDevicesListener(devicesChangeObserver)
        deviceManager.unregisterAdbServerStatusListener(adbStatusChangeObserver)
    }
}