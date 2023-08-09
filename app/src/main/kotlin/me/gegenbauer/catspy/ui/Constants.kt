package me.gegenbauer.catspy.ui

import me.gegenbauer.catspy.common.ui.icon.iconTabFileLog
import me.gegenbauer.catspy.common.ui.icon.iconTabHome
import me.gegenbauer.catspy.common.ui.icon.iconTabScript
import me.gegenbauer.catspy.log.ui.LogTabPanel
import me.gegenbauer.catspy.script.ui.ScriptTabPanel
import me.gegenbauer.catspy.ui.panel.HomePanel
import me.gegenbauer.catspy.ui.panel.TabInfo
import me.gegenbauer.catspy.utils.loadIcon

val supportedTabs = listOf(
    TabInfo("Log", iconTabFileLog, LogTabPanel::class.java),
    TabInfo("Script", iconTabScript, ScriptTabPanel::class.java),
)
