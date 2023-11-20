package me.gegenbauer.catspy.filter.ui

import me.gegenbauer.catspy.utils.Key
import me.gegenbauer.catspy.utils.KeyEventInterceptor
import org.fife.ui.autocomplete.BasicCompletion
import org.fife.ui.autocomplete.CompletionProvider
import org.fife.ui.autocomplete.DefaultCompletionProvider
import javax.swing.text.JTextComponent

class AutoCompleteHelper {
    private val suggestions = arrayListOf<String>()
    private var textComponent: JTextComponent? = null
    private var keyInterceptor: KeyEventInterceptor? = null
    private var autoCompletion: FilterAutoCompletion? = null

    fun enableAutoComplete(
        textComponent: JTextComponent,
        suggestions: List<String>,
        onCompletionsShown: (() -> Unit)? = null
    ) {
        disableAutoComplete()

        this.suggestions.clear()
        this.suggestions.addAll(suggestions)
        this.textComponent = textComponent

        autoCompletion = FilterAutoCompletion(createCompletionProvider(suggestions), onCompletionsShown).apply {
            isAutoCompleteEnabled = true
            isAutoActivationEnabled = true
            autoCompleteSingleChoices = false
            install(textComponent)
        }

        keyInterceptor = KeyEventInterceptor(textComponent, Key.TAB).also {
            it.enable { autoCompletion?.insertCurrentCompletion() }
        }
    }

    fun disableAutoComplete() {
        keyInterceptor?.disable()
        keyInterceptor = null
        autoCompletion?.uninstall()
        autoCompletion = null
        textComponent = null
    }
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
    return DefaultCompletionProvider().apply {
        setAutoActivationRules(false, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ\n")
        addCompletions(suggestions.map { BasicCompletion(this, it) })
    }
}