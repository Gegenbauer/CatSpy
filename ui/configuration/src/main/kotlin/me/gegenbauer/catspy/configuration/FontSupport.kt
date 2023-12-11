package me.gegenbauer.catspy.configuration

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.extras.FlatAnimatedLafChange
import com.formdev.flatlaf.util.FontUtils
import me.gegenbauer.catspy.common.Resources
import me.gegenbauer.catspy.glog.GLog
import java.awt.Font
import java.awt.GraphicsEnvironment
import javax.swing.UIManager
import javax.swing.plaf.FontUIResource
import javax.swing.text.StyleContext


object FontSupport {
    private const val TAG = "FontSupport"

    private val FONT_HACK: Font? = openFontTTF("Hack-Regular")

    fun registerBundledFonts() {
        val grEnv = GraphicsEnvironment.getLocalGraphicsEnvironment()
        FONT_HACK?.let { grEnv.registerFont(it) }
    }

    fun loadByStr(fontDesc: String): Font {
        val parts = fontDesc.split("/")
        require(parts.size == 3) { "Unsupported font description format: $fontDesc" }

        val (name, styleStr, sizeStr) = parts
        val style = parseFontStyle(styleStr)
        val size = sizeStr.toInt()

        val sc = StyleContext.getDefaultStyleContext()
        return sc.getFont(name, style, size) ?: throw RuntimeException("Font not found: $fontDesc")
    }

    fun convertToStr(font: Font): String {
        require(font.size >= 1) { "Bad font size: ${font.size}" }
        return "${font.fontName}/${convertFontStyleToString(font.style)}/${font.size}"
    }

    fun convertFontStyleToString(style: Int): String {
        return when (style) {
            Font.PLAIN -> "plain"
            Font.BOLD -> "bold"
            Font.ITALIC -> "italic"
            Font.BOLD or Font.ITALIC -> "bolditalic"
            else -> "unknown"
        }
    }

    private fun parseFontStyle(str: String): Int {
        var style = Font.PLAIN
        if ("bold" in str) {
            style = style or Font.BOLD
        }
        if ("italic" in str) {
            style = style or Font.ITALIC
        }
        return style
    }

    private fun openFontTTF(name: String): Font? {
        val fontPath = "fonts/$name.ttf"
        return try {
            Resources.loadResourceAsStream(fontPath).use {
                Font.createFont(Font.TRUETYPE_FONT, it)?.deriveFont(12f)
            }
        } catch (e: Exception) {
            GLog.e(TAG, "[openFontTTF] failed load font by path $fontPath", e)
            null
        }
    }

    fun setUIFont(font: Font) {
        UIManager.getDefaults().asSequence().filter { it.value is FontUIResource }.forEach { entry ->
            UIManager.put("defaultFont", FontUtils.getCompositeFont(font.family, font.style, font.size))
        }
    }
}