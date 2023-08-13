package me.gegenbauer.catspy.ui

import me.gegenbauer.catspy.iconset.GIcons
import me.gegenbauer.catspy.log.ui.LogTabPanel
import me.gegenbauer.catspy.script.ui.ScriptTabPanel
import me.gegenbauer.catspy.ui.panel.TabInfo
import me.gegenbauer.catspy.utils.TAB_ICON_SIZE

val supportedTabs = listOf(
    TabInfo("FileLog", GIcons.Tab.FileLog.get(TAB_ICON_SIZE, TAB_ICON_SIZE), LogTabPanel::class.java),
    TabInfo("Script", GIcons.Tab.Script.get(TAB_ICON_SIZE, TAB_ICON_SIZE), ScriptTabPanel::class.java),
)
