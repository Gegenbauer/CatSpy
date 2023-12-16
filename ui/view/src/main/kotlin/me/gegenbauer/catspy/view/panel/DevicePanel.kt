package me.gegenbauer.catspy.view.panel

import com.github.weisj.darklaf.iconset.AllIcons
import com.malinskiy.adam.request.device.Device
import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.ddmlib.device.AdamDeviceManager
import me.gegenbauer.catspy.ddmlib.device.DeviceListener
import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.view.button.IconBarButton
import java.awt.Component
import javax.swing.*

class DevicePanel : JPanel(), DeviceListener {
    private val refreshButton: JButton = IconBarButton(AllIcons.Action.Refresh.get())
    private val adbStatusTitle: JLabel = JLabel("ADB Server Status")
    private val adbStatus: JLabel = JLabel("Unknown")
    private val deviceList: JList<String> = JList(DefaultListModel())
    private val deviceManager by lazy { ServiceManager.getContextService(AdamDeviceManager::class.java) }

    init {
        border = BorderFactory.createEmptyBorder(10, 4, 10, 4)

        val p = TableLayoutConstants.PREFERRED
        val f = TableLayoutConstants.FILL
        layout = TableLayout(
            doubleArrayOf(f, p), // Columns
            doubleArrayOf(p, p, f) // Rows
        )

        // Add refresh button to the top right corner
        add(refreshButton, "1, 0")

        // Add adb status label below the refresh button
        add(adbStatusTitle, "0, 1")
        add(adbStatus, "1, 1")

        adbStatus.border = BorderFactory.createEmptyBorder(0, 20, 0, 0)

        // Add device list below the adb status label
        add(JScrollPane(deviceList), "0, 2, 1, 2")

        deviceManager.registerDeviceListener(this)
        val devices = deviceManager.getDevices()
        devices.forEach {
            (deviceList.model as DefaultListModel<String>).addElement(it.serial)
        }

        adbStatus.text = if (deviceManager.isAdbServerRunning) "Running" else "Stopped"

        refreshButton.addActionListener {
            val devices = deviceManager.getDevices()
            (deviceList.model as DefaultListModel<String>).clear()
            devices.forEach {
                (deviceList.model as DefaultListModel<String>).addElement(it.serial)
            }
            adbStatus.text = if (deviceManager.isAdbServerRunning) "Running" else "Stopped"
        }

        deviceList.requestFocus()
    }

    override fun onDeviceConnect(device: Device) {
        (deviceList.model as DefaultListModel<String>).addElement(device.serial)
    }

    override fun onDeviceDisconnect(device: Device) {
        (deviceList.model as DefaultListModel<String>).removeElement(device.serial)
    }

    override fun onDeviceStateChange(deviceOld: Device, deviceNew: Device) {
        // Do nothing
    }
}

class DeviceIcon: StatusIcon {
    override val icon: Icon = GIcons.Device.Phone.get(20, 20)
    override val onClick: () -> Unit = ::showDevicePanel

    override var state: StatusIcon.State = StatusIcon.State.NORMAL
    override var host: Component? = null

    private val devicePanel = DevicePanel()
    private val deviceDialog = JDialog().apply {
        isResizable = true
        defaultCloseOperation = JDialog.HIDE_ON_CLOSE
    }

    private fun showDevicePanel() {
        if (deviceDialog.isVisible) {
            deviceDialog.isVisible = false
            return
        }
        deviceDialog.contentPane = devicePanel
        deviceDialog.pack()
        val location = this.host?.locationOnScreen ?: return
        location.y -= deviceDialog.height + 30
        location.x -= deviceDialog.width / 2
        deviceDialog.location = location
        deviceDialog.isVisible = true
    }
}