package me.gegenbauer.catspy.configuration

import com.github.weisj.darklaf.theme.*
import com.github.weisj.darklaf.theme.spec.AccentColorRule
import com.github.weisj.darklaf.theme.spec.FontPrototype
import com.github.weisj.darklaf.theme.spec.FontSizeRule
import me.gegenbauer.catspy.utils.toArgb
import java.awt.Font

const val DEFAULT_FONT_SIZE = 14
const val DEFAULT_LOG_FONT_SIZE = 13
const val DEFAULT_FONT_STYLE = 0
const val DEFAULT_FONT_FAMILY = "Dialog"
const val DEFAULT_FONT_SCALE_PERCENTAGE = 100
const val DEFAULT_THEME = "One Dark"

data class GTheme(
    val theme: String = DEFAULT_THEME,
    val fontScalePercentage: Int = DEFAULT_FONT_SCALE_PERCENTAGE,
    val fontFamily: String = DEFAULT_FONT_FAMILY,
    val accentColor: Int = 0,
    val selectionColor: Int = 0,
    val isSystemPreferencesEnabled: Boolean = false,
    val isAccentColorFollowsSystem: Boolean = false,
    val isSelectionColorFollowsSystem: Boolean = false,
    val isThemeFollowsSystem: Boolean = false,
)

private val themes = arrayListOf(
    DarculaTheme(),
    HighContrastDarkTheme(),
    HighContrastLightTheme(),
    IntelliJTheme(),
    OneDarkTheme(),
    SolarizedDarkTheme(),
    SolarizedLightTheme()
)

fun getTheme(name: String?): Theme {
    name ?: return themes.first()
    return themes.firstOrNull { it.name == name } ?: themes.first()
}

fun GTheme.fontSizeRule(): FontSizeRule? {
    if (fontScalePercentage == 0) return null
    return FontSizeRule.relativeAdjustment(fontScalePercentage)
}

fun GTheme.accentColorRule(): AccentColorRule? {
    if (accentColor == 0 || selectionColor == 0) return null
    return AccentColorRule.fromColor(accentColor.toArgb(), selectionColor.toArgb())
}

fun GTheme.fontPrototype(): FontPrototype? {
    if (fontFamily.isBlank()) return null
    return FontPrototype(fontFamily)
}

fun Theme.toFont(): Font {
    return Font(fontPrototype.family(), 0, (DEFAULT_FONT_SIZE.toFloat() * fontSizeRule.percentage / 100).toInt())
}

fun Font.newFont(theme: Theme = ThemeManager.currentTheme, baseFontSize: Int): Font {
    return Font(
        theme.fontPrototype.family(), 0, (baseFontSize.toDouble() *
                theme.fontSizeRule.percentage / 100).toInt()
    )
}

fun Font.newFont(size: Int = getSize(), family: String = getFamily(), style: Int = getStyle()): Font {
    return Font(family, style, size)
}