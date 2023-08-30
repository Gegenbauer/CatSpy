package me.gegenbauer.catspy.script.ui

import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.databinding.bind.componentName
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.view.card.Card
import me.gegenbauer.catspy.view.card.RoundedCard
import java.awt.GridLayout
import java.awt.event.ComponentAdapter
import java.util.*
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JPanel

class ScriptCardContainer(override val contexts: Contexts = Contexts.default) : CardContainer, Context {
    override val container: JPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        componentName = "ScriptCardContainer"
    }

    private val cards = PriorityQueue<Card> { c1, c2 -> c1.id - c2.id }
    private var span = 5

    init {
        container.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: java.awt.event.ComponentEvent) {
                span = (container.width / CARD_WIDTH_THRESHOLD).coerceAtLeast(1)
                container.components.forEach {
                    (it as JPanel).layout = GridLayout(0, span)
                }
            }
        })
        container.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    }

    private fun createRowPanel(): JPanel = JPanel(GridLayout(0, span)).apply {
        componentName = "RowPanel"
    }

    override fun addCard(card: Card) {
        cards.add(card)
        if (card is RoundedCard) {
            ThemeManager.registerThemeUpdateListener(card)
        }
        val rowPanel = if (cards.size % span == 1) {
            createRowPanel().apply {
                container.add(this)
            }
        } else {
            container.getComponent(cards.size / span) as JPanel
        }
        rowPanel.add(card.component)
    }

    override fun removeCard(card: Card) {
        if (cards.contains(card).not()) {
            GLog.w(TAG, "[removeCard] card has not been added: $card")
        }
        cards.remove(card)
        if (card is RoundedCard) {
            ThemeManager.unregisterThemeUpdateListener(card)
        }
        val rowPanel = container.getComponent(cards.size / span) as JPanel
        rowPanel.remove(card.component)
        if (rowPanel.componentCount == 0) {
            container.remove(rowPanel)
        }
    }

    override fun getCards(): List<Card> {
        return cards.toList()
    }

    override fun updateAllCards() {
        cards.forEach(Card::updateContent)
    }

    override fun setAutomaticallyUpdate(enabled: Boolean) {
        cards.forEach { it.setAutomaticallyUpdate(enabled) }
    }

    override fun stopAutomaticallyUpdate() {
        cards.forEach(Card::stopAutomaticallyUpdate)
    }

    override fun resumeAutomaticallyUpdate() {
        cards.forEach(Card::resumeAutomaticallyUpdate)
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        cards.forEach { it.setContexts(contexts) }
    }

    override fun destroy() {
        cards.forEach(Card::destroy)
    }

    companion object {
        private const val TAG = "ScriptCardContainer"
        private const val CARD_WIDTH_THRESHOLD = 1200
    }
}