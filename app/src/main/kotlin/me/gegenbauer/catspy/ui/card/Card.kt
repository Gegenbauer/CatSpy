package me.gegenbauer.catspy.ui.card

interface Card {
    val id: Int

    val component: RoundedCard

    fun updateContent()

    fun setAutomaticallyUpdate(enabled: Boolean)

    fun stopAutomaticallyUpdate()

    fun resumeAutomaticallyUpdate()

    fun setPeriod(period: Long)
}