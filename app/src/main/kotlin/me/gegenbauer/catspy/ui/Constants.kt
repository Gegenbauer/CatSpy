package me.gegenbauer.catspy.ui

import me.gegenbauer.catspy.log.ui.LogTabPanel
import me.gegenbauer.catspy.script.ui.ScriptTabPanel
import me.gegenbauer.catspy.ui.panel.TabInfo

val supportedTabs = listOf(
    TabInfo("Log", null, LogTabPanel::class.java),
    TabInfo("Script", null, ScriptTabPanel::class.java),
)