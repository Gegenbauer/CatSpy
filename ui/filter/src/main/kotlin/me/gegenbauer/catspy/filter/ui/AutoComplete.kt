package me.gegenbauer.catspy.filter.ui

import me.gegenbauer.catspy.utils.interceptEvent
import org.fife.ui.autocomplete.BasicCompletion
import org.fife.ui.autocomplete.CompletionProvider
import java.awt.event.KeyEvent
import javax.swing.text.JTextComponent

fun JTextComponent.enableAutoComplete(suggestions: List<String>) {
    val provider = createCompletionProvider(suggestions)
    val ac = FilterAutoCompletion(provider)

    // disable enter key
    interceptEvent(this, KeyEvent.VK_ENTER, KeyEvent.KEY_PRESSED) {
        ac.insertCurrentCompletion()
    }
    // A CompletionProvider is what knows of all possible completions, and
    // analyzes the contents of the text area at the caret position to
    // determine what completion choices should be presented. Most instances
    // of CompletionProvider (such as DefaultCompletionProvider) are designed
    // so that they can be shared among multiple text components.


    // An AutoCompletion acts as a "middle-man" between a text component
    // and a CompletionProvider. It manages any options associated with
    // the auto-completion (the popup trigger key, whether to display a
    // documentation window along with completion choices, etc.). Unlike
    // CompletionProviders, instances of AutoCompletion cannot be shared
    // among multiple text components.
    ac.isAutoCompleteEnabled = true
    ac.isAutoActivationEnabled = true
    ac.install(this)
}

/**
 * Create a simple provider that adds some Java-related completions.
 */
private fun createCompletionProvider(suggestions: List<String>): CompletionProvider {

    // A DefaultCompletionProvider is the simplest concrete implementation
    // of CompletionProvider. This provider has no understanding of
    // language semantics. It simply checks the text entered up to the
    // caret position for a match against known completions. This is all
    // that is needed in the majority of cases.
    return FilterCompletionProvider().apply {
        setAutoActivationRules(false, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
        addCompletions(suggestions.map { BasicCompletion(this, it) })
    }
}