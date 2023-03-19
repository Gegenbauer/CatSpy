package me.gegenbauer.logviewer.theme

import com.github.weisj.darklaf.theme.*
import com.github.weisj.darklaf.theme.spec.AccentColorRule
import com.github.weisj.darklaf.theme.spec.FontSizeRule
import me.gegenbauer.logviewer.utils.toArgb

data class GTheme(
    val theme: String,
    val fontScalePercentage: Int,
    val accentColor: Int,
    val selectionColor: Int,
    val isSystemPreferencesEnabled: Boolean,
    val isAccentColorFollowsSystem: Boolean,
    val isSelectionColorFollowsSystem: Boolean,
    val isThemeFollowsSystem: Boolean
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
    return themes.first { it.name == name }
}

fun GTheme.fontSizeRule(): FontSizeRule? {
    if (fontScalePercentage == 0) return null
    return FontSizeRule.relativeAdjustment(fontScalePercentage)
}

fun GTheme.accentColorRule(): AccentColorRule? {
    if (accentColor == 0 || selectionColor == 0) return null
    return AccentColorRule.fromColor(accentColor.toArgb(), selectionColor.toArgb())
}