package me.gegenbauer.logviewer.theme

import com.github.weisj.darklaf.theme.*

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