package me.gegenbauer.catspy.configuration

object DarkLightThemes {
    private val lightToDarkThemes = mapOf(
        "FlatLaf Light" to "FlatLaf Dark",
        "FlatLaf macOS Light" to "FlatLaf macOS Dark",
        "FlatLaf IntelliJ" to "FlatLaf Darcula",
        "Arc" to "Arc Dark",
        "Arc - Orange" to "Arc Dark - Orange",
        "Light Flat" to "Dark Flat",
        "Solarized Light" to "Solarized Dark",
        "Atom One Dark (Material)" to "Atom One Light (Material)",
        "GitHub (Material)" to "GitHub Dark (Material)",
        "Light Owl (Material)" to "Night Owl (Material)",
        "Solarized Light (Material)" to "Solarized Dark (Material)",
        "Material Lighter (Material)" to "Material Darker (Material)",
    )

    fun getLightTheme(darkTheme: String): String {
        return lightToDarkThemes.entries.find { it.value == darkTheme }?.key ?: ""
    }

    fun getDarkTheme(lightTheme: String): String {
        return lightToDarkThemes[lightTheme] ?: ""
    }
}