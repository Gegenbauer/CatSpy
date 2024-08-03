package me.gegenbauer.catspy.view.tab

import me.gegenbauer.catspy.utils.IdGenerator
import javax.swing.Icon

data class FunctionTab(
    val name: String,
    val icon: Icon?,
    val clazz: Class<out TabPanel>,
    val tooltip: String? = null,
    val closeable: Boolean = true,
    val id: Int = IdGenerator.generateId()
)