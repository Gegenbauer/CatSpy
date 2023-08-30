package me.gegenbauer.catspy.script.ui

import me.gegenbauer.catspy.view.card.Card
import javax.swing.JPanel

interface CardContainer {
    val container: JPanel

    fun addCard(card: Card)

    fun removeCard(card: Card)

    fun getCards(): List<Card>

    fun updateAllCards()

    fun setAutomaticallyUpdate(enabled: Boolean)

    fun stopAutomaticallyUpdate()

    fun resumeAutomaticallyUpdate()
}