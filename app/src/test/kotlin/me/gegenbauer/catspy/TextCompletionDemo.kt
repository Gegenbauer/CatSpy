package me.gegenbauer.catspy

import com.github.weisj.darklaf.LafManager
import com.github.weisj.darklaf.theme.Theme
import com.github.weisj.darklaf.ui.button.DarkButtonUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.gegenbauer.catspy.configuration.ThemeManager
import me.gegenbauer.catspy.configuration.GlobalConfSync
import me.gegenbauer.catspy.concurrency.APP_LAUNCH
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.UI
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.userDir
import org.fife.ui.autocomplete.*
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import java.awt.Dimension
import javax.swing.*
import javax.swing.text.AbstractDocument
import javax.swing.text.DocumentFilter


fun main() {
    AppScope.launch(Dispatchers.UI) {
        withContext(Dispatchers.APP_LAUNCH) {
            GLog.init(userDir, "glog.txt")
            GLog.debug = true
            ThemeManager.init()
            GlobalConfSync.init()
        }
        ThemeManager.installTheme()
        LafManager.registerInitTask { _: Theme, defaults: UIDefaults ->
            defaults[DarkButtonUI.KEY_VARIANT] = DarkButtonUI.VARIANT_BORDERLESS
        }
        val frame = AutoCompleteDemo()
        frame.size = Dimension(500, 500)
        frame.isVisible = true
    }
}


class AutoCompleteDemo : JFrame() {
    init {
        val textArea = RSyntaxTextArea()
        textArea.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_NONE
        textArea.lineWrap = false
        textArea.wrapStyleWord = false
        // max text area can only display one line
        (textArea.document as AbstractDocument).apply {
            val oldFilter = documentFilter
            val newFilter = object : DocumentFilter() {
                override fun insertString(fb: FilterBypass?, offset: Int, string: String?, attr: javax.swing.text.AttributeSet?) {
                    if (string == "\n") {
                        println("enter")
                        return
                    }
                    oldFilter.insertString(fb, offset, string, attr)
                }
            }
            documentFilter = newFilter
        }
        val contentPane = JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        contentPane.maximumSize = Dimension(Int.MAX_VALUE, 20)
        // A CompletionProvider is what knows of all possible completions, and
        // analyzes the contents of the text area at the caret position to
        // determine what completion choices should be presented. Most instances
        // of CompletionProvider (such as DefaultCompletionProvider) are designed
        // so that they can be shared among multiple text components.
        val provider = createCompletionProvider()

        // An AutoCompletion acts as a "middle-man" between a text component
        // and a CompletionProvider. It manages any options associated with
        // the auto-completion (the popup trigger key, whether to display a
        // documentation window along with completion choices, etc.). Unlike
        // CompletionProviders, instances of AutoCompletion cannot be shared
        // among multiple text components.
        val ac = AutoCompletion(provider)
        ac.isAutoCompleteEnabled = true
        ac.isAutoActivationEnabled = true
        ac.install(textArea)
        setContentPane(contentPane)
        title = "AutoComplete Demo"
        defaultCloseOperation = EXIT_ON_CLOSE
        pack()
        setLocationRelativeTo(null)
    }

    /**
     * Create a simple provider that adds some Java-related completions.
     */
    private fun createCompletionProvider(): CompletionProvider {

        // A DefaultCompletionProvider is the simplest concrete implementation
        // of CompletionProvider. This provider has no understanding of
        // language semantics. It simply checks the text entered up to the
        // caret position for a match against known completions. This is all
        // that is needed in the majority of cases.
        return DefaultCompletionProvider().apply {
            setAutoActivationRules(false, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
            addCompletion(BasicCompletion(this, "abstract"))
            addCompletion(BasicCompletion(this, "abandon"))
            addCompletion(BasicCompletion(this, "abuse"))

            addCompletion(FunctionCompletion(this, "getDisplayContent", "String"))
            addCompletion(FunctionCompletion(this, "getValueType", "String"))
            addCompletion(FunctionCompletion(this, "getFirstChar", "String"))

            addCompletion(ShorthandCompletion(this, "g", "getFirstChar"))
            addCompletion(ShorthandCompletion(this, "f", "getFirstChar"))
            addCompletion(ShorthandCompletion(this, "c", "getFirstChar"))
            addCompletion(ShorthandCompletion(this, "gf", "getFirstChar"))
            addCompletion(ShorthandCompletion(this, "gfc", "getFirstChar"))
            addCompletion(ShorthandCompletion(this, "fc", "getFirstChar"))

            addCompletion(VariableCompletion(this, "firstChar", "String"))
            addCompletion(VariableCompletion(this, "isFirstChar", "Boolean"))
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // Instantiate GUI on the EDT.
            SwingUtilities.invokeLater {
                try {
                    val laf = UIManager.getSystemLookAndFeelClassName()
                    UIManager.setLookAndFeel(laf)
                } catch (e: Exception) { /* Never happens */
                }
                AutoCompleteDemo().isVisible = true
            }
        }
    }
}