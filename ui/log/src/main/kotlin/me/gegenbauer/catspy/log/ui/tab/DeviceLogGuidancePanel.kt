package me.gegenbauer.catspy.log.ui.tab

import info.clearthought.layout.TableLayout
import info.clearthought.layout.TableLayoutConstants.FILL
import info.clearthought.layout.TableLayoutConstants.PREFERRED
import me.gegenbauer.catspy.strings.STRINGS
import me.gegenbauer.catspy.view.button.GButton
import me.gegenbauer.catspy.view.panel.HorizontalFlexibleHeightLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

class DeviceLogGuidancePanel(
    private val onOpenDeviceLogPanelClick: () -> Unit
): JPanel() {

    private val openDeviceLogPanelButton = GButton(STRINGS.ui.openAndroidDeviceLogPanel).apply {
        addActionListener {
            onOpenDeviceLogPanelClick()
        }
    }

    init {
        border = BorderFactory.createTitledBorder(STRINGS.ui.deviceLog)

        val actionPanel = JPanel(HorizontalFlexibleHeightLayout())
        actionPanel.border = BorderFactory.createEmptyBorder(6, 10, 6, 0)
        actionPanel.add(openDeviceLogPanelButton)

        layout = TableLayout(
            arrayOf(
                doubleArrayOf(FILL),
                doubleArrayOf(PREFERRED)
            )
        )

        add(actionPanel, "0,0")
    }
}