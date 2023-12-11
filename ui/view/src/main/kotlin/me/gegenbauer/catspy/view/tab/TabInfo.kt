package me.gegenbauer.catspy.view.tab

import javax.swing.Icon

data class TabInfo(
    val tabName: String,
    val tabIcon: Icon?,
    val tabClazz: Class<out TabPanel>,
    val tooltip: String? = null
)