package me.gegenbauer.catspy.filter.ui

import me.gegenbauer.catspy.utils.DefaultDocumentListener
import me.gegenbauer.catspy.utils.getFieldDeeply
import me.gegenbauer.catspy.utils.invokeMethod
import org.fife.ui.autocomplete.AutoCompletion
import org.fife.ui.autocomplete.Completion
import org.fife.ui.autocomplete.CompletionProvider
import javax.swing.JWindow
import javax.swing.event.DocumentEvent
import javax.swing.text.JTextComponent

class FilterAutoCompletion(provider: CompletionProvider) : AutoCompletion(provider) {

    fun insertCurrentCompletion() {
        val completion = getCurrentSelectedCompletion()
        completion?.let { insertCompletion(it) }
    }

    override fun install(c: JTextComponent) {
        super.install(c)
        c.document.addDocumentListener(object : DefaultDocumentListener() {
            override fun insertUpdate(e: DocumentEvent) {
                if (isAutoCompleteEnabled && isAutoActivationEnabled) {
                    refreshPopupWindow()
                }
            }
        })
    }

    private fun getCurrentSelectedCompletion(): Completion? {
        val popupWindow = getPopupWindow()
        return if (popupWindow != null && popupWindow.isVisible) {
            val result = popupWindow.invokeMethod("getSelection") as? Result<Completion>
            result?.getOrNull()
        } else {
            null
        }
    }

    private fun getPopupWindow(): JWindow? {
        val popupField = getFieldDeeply("popupWindow")
        return popupField.get(this) as? JWindow
    }
}