package me.gegenbauer.catspy.script.ui

import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.DeviceState
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceMonitor
import me.gegenbauer.catspy.java.ext.Bundle
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import me.gegenbauer.catspy.script.model.Script
import me.gegenbauer.catspy.script.model.ScriptType
import me.gegenbauer.catspy.script.parser.DirectRule
import me.gegenbauer.catspy.script.parser.RegexRule
import me.gegenbauer.catspy.script.task.ScriptTaskManager
import me.gegenbauer.catspy.view.tab.BaseTabPanel
import java.awt.BorderLayout
import javax.swing.JComponent

class ScriptTabPanel : BaseTabPanel() {

    override val tag: String = "ScriptTabPanel"

    private val taskManager = ServiceManager.getContextService(ScriptTaskManager::class.java)
    private val cardContainer = ScriptCardContainer()

    private val focusedActivityParseRule = RegexRule("mCurrentFocus=Window\\{[0-9a-z]+ [0-9a-z]+ (.*)\\}", DirectRule())
    private val getFocusedActivityScript = Script(
        "GetFocusedActivity",
        ScriptType.adb,
        "dumpsys activity activities"
    )
    private val buildTimeScript = Script(
        "BuildTime",
        ScriptType.adb,
        "getprop ro.build.inside.id"
    )
    private val windowSize = Script(
        "WindowSize",
        ScriptType.adb,
        "wm size"
    )
    private val windowDensity = Script(
        "WindowDensity",
        ScriptType.adb,
        "wm density"
    )
    private val windowOrientation = Script(
        "WindowOrientation",
        ScriptType.adb,
        "dumpsys input | grep 'SurfaceOrientation'"
    )
    private val windowState = Script(
        "WindowState",
        ScriptType.adb,
        "dumpsys window | grep 'mCurrentFocus'"
    )
    private val windowResolution = Script(
        "WindowResolution",
        ScriptType.adb,
        "dumpsys window | grep 'DisplayWidth'"
    )
    private val focusedActivityCard = ScriptCard(taskManager, ScriptUIItem(getFocusedActivityScript, focusedActivityParseRule))
    private val deviceInfoCard = DeviceInfoCard(
        listOf(
            ScriptUIItem(getFocusedActivityScript, focusedActivityParseRule),
            ScriptUIItem(windowSize),
            ScriptUIItem(buildTimeScript),
            ScriptUIItem(windowDensity),
            ScriptUIItem(windowOrientation),
            ScriptUIItem(windowState),
            ScriptUIItem(windowResolution)
        ),
        taskManager
    )

    var currentDevice: Device = defaultDevice
        set(value) {
            field = value
            focusedActivityCard.device = value
        }

    override fun onSetup(bundle: Bundle?) {
        val deviceManager = ServiceManager.getContextService(AdamDeviceMonitor::class.java)
        deviceManager.tryStartMonitor()

        layout = BorderLayout()
        add(cardContainer.container, BorderLayout.NORTH)

        cardContainer.addCard(focusedActivityCard)
        cardContainer.addCard(deviceInfoCard)

        cardContainer.setAutomaticallyUpdate(true)
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        focusedActivityCard.setParent(this)
        currentDevice = ServiceManager.getContextService(AdamDeviceMonitor::class.java)
            .getDevices().firstOrNull() ?: currentDevice
    }

    override fun onTabSelected() {
        taskManager.updatePauseState(false)
    }

    override fun onTabUnselected() {
        taskManager.updatePauseState(true)
    }

    override fun destroy() {
        super.destroy()
        taskManager.cancelAll()
        val deviceManager = ServiceManager.getContextService(AdamDeviceMonitor::class.java)
        deviceManager.tryStopMonitor()
    }

    private fun updateCardContent() {
        focusedActivityCard.updateContent()
    }


    companion object {
        val defaultDevice = Device(EMPTY_STRING, DeviceState.DEVICE)
    }
}