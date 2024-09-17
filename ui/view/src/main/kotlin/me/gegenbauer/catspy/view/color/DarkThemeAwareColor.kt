package me.gegenbauer.catspy.view.color

import me.gegenbauer.catspy.configuration.ThemeManager
import java.awt.Color
import java.util.Objects

data class DarkThemeAwareColor(
    val dayColor: Color,
    val nightColor: Color = dayColor
) : ColorWrapper() {

    private val isNightTheme: Boolean
        get() = ThemeManager.isDarkTheme

    override val color: Color
        get() = if (isNightTheme) nightColor else dayColor

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DarkThemeAwareColor) return false

        if (dayColor != other.dayColor) return false
        if (nightColor != other.nightColor) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(dayColor, nightColor)
    }
}