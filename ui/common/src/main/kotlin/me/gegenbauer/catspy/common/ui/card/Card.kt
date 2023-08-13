package me.gegenbauer.catspy.common.ui.card

import me.gegenbauer.catspy.context.Context
import javax.swing.JComponent

interface Card: Context {
    val id: Int

    val component: JComponent

    fun updateContent()

    fun setAutomaticallyUpdate(enabled: Boolean)

    fun stopAutomaticallyUpdate()

    fun resumeAutomaticallyUpdate()

    fun setPeriod(period: Long)
}