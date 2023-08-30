package me.gegenbauer.catspy.filter.ui

import org.fife.ui.autocomplete.Completion
import org.fife.ui.autocomplete.DefaultCompletionProvider
import javax.swing.text.JTextComponent

class FilterCompletionProvider: DefaultCompletionProvider() {
    override fun getCompletionsImpl(comp: JTextComponent): MutableList<Completion> {
        val retVal: List<Completion> = ArrayList()
        val text = getAlreadyEnteredText(comp)
        if (text.isNullOrEmpty()) {
            return retVal.toMutableList()
        }
        return completions.filter { it.inputText.contains(text, true) }.toMutableList()
    }
}