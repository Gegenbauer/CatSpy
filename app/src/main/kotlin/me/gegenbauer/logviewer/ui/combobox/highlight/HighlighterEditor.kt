package me.gegenbauer.logviewer.ui.combobox.highlight

import me.gegenbauer.logviewer.manager.ColorManager
import me.gegenbauer.logviewer.ui.FilterComboBox.fontBackgroundInclude
import java.awt.Color
import javax.swing.plaf.basic.BasicComboBoxEditor
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter
import javax.swing.text.Highlighter
import javax.swing.text.JTextComponent

abstract class HighlighterEditor : BasicComboBoxEditor() {

    var useColorTag: Boolean = true
    var errorMsg: String = ""
    var isHighlightEnabled = true

    protected fun updateHighlighter() {
        val textComponent = editorComponent as JTextComponent
        val painterInclude: Highlighter.HighlightPainter = DefaultHighlighter.DefaultHighlightPainter(fontBackgroundInclude)
        val painterExclude: Highlighter.HighlightPainter = DefaultHighlighter.DefaultHighlightPainter(ColorManager.filterStyleExclude)
        val painterSeparator: Highlighter.HighlightPainter = DefaultHighlighter.DefaultHighlightPainter(ColorManager.filterStyleSeparator)
        val text = textComponent.text
        val separator = "|"
        try {
            textComponent.highlighter.removeAllHighlights()
            var currPos = 0
            while (currPos < text.length) {
                val startPos = currPos
                val separatorPos = text.indexOf(separator, currPos)
                var endPos = separatorPos
                if (separatorPos < 0) {
                    endPos = text.length
                }
                if (startPos in 0 until endPos) {
                    if (text[startPos] == '-') {
                        textComponent.highlighter.addHighlight(startPos, endPos, painterExclude)
                    } else if (useColorTag && text[startPos] == '#' && startPos < (endPos - 1) && text[startPos + 1].isDigit()) {
                        val color =
                            Color.decode(ColorManager.filterTableColor.strFilteredBGs[text[startPos + 1].digitToInt()])
                        val painterColor: Highlighter.HighlightPainter =
                            DefaultHighlighter.DefaultHighlightPainter(color)
                        textComponent.highlighter.addHighlight(startPos, startPos + 2, painterColor)
                        textComponent.highlighter.addHighlight(startPos + 2, endPos, painterInclude)
                    } else {
                        textComponent.highlighter.addHighlight(startPos, endPos, painterInclude)
                    }
                }
                if (separatorPos >= 0) {
                    textComponent.highlighter.addHighlight(separatorPos, separatorPos + 1, painterSeparator)
                }
                currPos = endPos + 1
            }
        } catch (ex: BadLocationException) {
            ex.printStackTrace()
        }
    }

    protected fun removeHighlighter() {
        val textComponent = editorComponent as JTextComponent
        textComponent.highlighter.removeAllHighlights()
    }
}