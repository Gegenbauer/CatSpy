package me.gegenbauer.catspy.script.ui

import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.DeviceState
import me.gegenbauer.catspy.common.ui.tab.OnTabChangeListener
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.componentName
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceManager
import me.gegenbauer.catspy.script.model.Script
import me.gegenbauer.catspy.script.model.ScriptType
import me.gegenbauer.catspy.script.parser.DirectRule
import me.gegenbauer.catspy.script.parser.RegexRule
import me.gegenbauer.catspy.task.TaskManager
import java.awt.BorderLayout
import javax.swing.JPanel

class ScriptMainUI(override val contexts: Contexts = Contexts.default) : JPanel(), Context, OnTabChangeListener {

    private val taskManager = TaskManager()
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

    init {
        componentName = "ScriptMainUI"
        layout = BorderLayout()
        add(cardContainer.container, BorderLayout.NORTH)

        cardContainer.addCard(focusedActivityCard)
        cardContainer.addCard(deviceInfoCard)

        cardContainer.setAutomaticallyUpdate(true)
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        focusedActivityCard.setContexts(contexts)
        currentDevice =
            ServiceManager.getContextService(AdamDeviceManager::class.java).getDevices().firstOrNull() ?: currentDevice
    }

    private fun updateCardContent() {
        focusedActivityCard.updateContent()
    }

    override fun onTabFocusChanged(focused: Boolean) {
        taskManager.updatePauseState(!focused)
    }


    companion object {
        val defaultDevice = Device("", DeviceState.DEVICE)
    }
}