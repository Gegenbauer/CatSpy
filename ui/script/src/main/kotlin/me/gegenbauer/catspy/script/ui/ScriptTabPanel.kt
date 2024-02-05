package me.gegenbauer.catspy.script.ui

import com.malinskiy.adam.request.device.Device
import com.malinskiy.adam.request.device.DeviceState
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceManager
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.script.model.Script
import me.gegenbauer.catspy.script.model.ScriptType
import me.gegenbauer.catspy.script.parser.DirectRule
import me.gegenbauer.catspy.script.parser.RegexRule
import me.gegenbauer.catspy.script.task.ScriptTaskManager
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.utils.TAB_ICON_SIZE
import me.gegenbauer.catspy.view.tab.TabPanel
import java.awt.BorderLayout
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JPanel

class ScriptTabPanel(override val contexts: Contexts = Contexts.default) : JPanel(), TabPanel {

    override val tabName: String = STRINGS.ui.tabScript
    override val tabIcon: Icon = GIcons.Tab.Script.get(TAB_ICON_SIZE, TAB_ICON_SIZE)

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

    override fun setup() {
        val deviceManager = ServiceManager.getContextService(AdamDeviceManager::class.java)
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
        currentDevice = ServiceManager.getContextService(AdamDeviceManager::class.java)
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
    }

    override fun getTabContent(): JComponent {
        return this
    }

    private fun updateCardContent() {
        focusedActivityCard.updateContent()
    }


    companion object {
        val defaultDevice = Device("", DeviceState.DEVICE)
    }
}