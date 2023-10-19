package me.gegenbauer.catspy.configuration

import com.github.weisj.darklaf.theme.Theme
import me.gegenbauer.catspy.iconset.GIcons
import java.awt.Color
import java.util.*
import javax.swing.Icon
import javax.swing.UIManager
import javax.swing.plaf.ColorUIResource

private val properties = UIManager.getDefaults()

object ToggleButtonTheme {
    val defaultIconUnselected: Icon = GIcons.State.ToggleOff.get()
    val defaultIconSelected: Icon = GIcons.State.ToggleOn.get()
}

object FilterComboBoxTheme {
    val fontBackgroundInclude: Color
        get() = properties.getColor("ComboBox.editBackground") ?: Color(255, 255, 255, 255)
    val fontBackgroundExclude = properties.getColor("ComboBox.selectionBackground") ?: Color(38F, 117F, 191F)
}

object EmptyStatePanelTheme {
    val iconBackground: Color
        get() = properties.getColor("Button.borderless.hover") ?: Color(242, 242, 242, 242)
}

object VStatusPanelTheme: ThemeAware {
    val backgroundDark: Color = Color(0x46494B)
    val backgroundLight: Color = Color(0xFFFFFF)
    val bookmarkLight: Color = Color(0x000000)
    val bookmarkDark: Color = Color(0xFFFFFF)
    val currentPositionDark: Color = Color(0xA0, 0xA0, 0xA0, 0x50)
    val currentPositionLight: Color = Color(0xC0, 0xC0, 0xC0, 0x50)

    override fun onThemeChanged(theme: Theme, properties: Hashtable<Any, Any>) {
        properties["VStatusPanel.background"] = if (Theme.isDark(theme)) ColorUIResource(backgroundDark) else ColorUIResource(
            backgroundLight
        )
        properties["VStatusPanel.bookmark"] = if (Theme.isDark(theme)) ColorUIResource(bookmarkDark) else ColorUIResource(
            bookmarkLight
        )
        properties["VStatusPanel.currentPosition"] = if (Theme.isDark(theme)) ColorUIResource(currentPositionDark) else ColorUIResource(
            currentPositionLight
        )
    }
}

object LogColorScheme: ThemeAware {
    private val DEFAULT_COLOR = Color(0x000000)
    data class ColorItem(val order: Int, val name: String, val color: Color)

    enum class TableColorIdx(val value: Int) {
        FILTERED_FG(0),
        SELECTED_BG(1),
        FILTERED_BG(2),
        LOG_BG(3),
        LINE_NUM_BG(4),
        BOOKMARK_BG(5),
        LOG_LEVEL_NONE(6),
        LOG_LEVEL_VERBOSE(7),
        LOG_LEVEL_DEBUG(8),
        LOG_LEVEL_INFO(9),
        LOG_LEVEL_WARNING(10),
        LOG_LEVEL_ERROR(11),
        LOG_LEVEL_FATAL(12),
        PID_FG(13),
        TID_FG(14),
        TAG_FG(15),
        HIGHLIGHT_FG(16),
        LINE_NUM_FG(17),
        NUM_LOG_SEPARATOR_BG(18),
        BOOKMARK_SELECTED_BG(19),
        NUM_BOOKMARK_SELECTED_BG(20),
        NUM_BOOKMARK_BG(21),
        NUM_SELECTED_BG(22),
        HIGHLIGHT_BG(23),
        FILTERED_START_FG(24),
        FILTERED_START_BG(33),
        SEARCH_FG(42),
        SEARCH_BG(43),
        FILER_COMBO_BOX_INCLUDE_BG(44),
        FILER_COMBO_BOX_EXCLUDE_BG(45),
        FILER_COMBO_BOX_SEPARATOR_BG(46),
    }

    private fun Color.alpha(alpha: Double): Color {
        return Color(red, green, blue, (alpha * 255).toInt())
    }

    private val colorArrayLight = arrayOf(
        ColorItem(0, "Filtered FG", Color(0xFFFFFF)),
        ColorItem(14, "Selected BG", Color(0xE0E0E0)),
        ColorItem(15, "Filtered BG", Color(0x20B020).alpha(0.3)),
        ColorItem(16, "Log BG", Color(0xFFFAFA)),
        ColorItem(17, "LineNum BG", Color(0xD0D0D0)),
        ColorItem(18, "Bookmark BG", Color(0xE0E0FF)),
        ColorItem(1, "Log Level None", Color(0x000000)),
        ColorItem(2, "Log Level Verbose", Color(0x000000)),
        ColorItem(3, "Log Level Debug", Color(0x209000)),
        ColorItem(4, "Log Level Info", Color(0x0080DF)),
        ColorItem(5, "Log Level Warning", Color(0xF07000)),
        ColorItem(6, "Log Level Error", Color(0xD00000)),
        ColorItem(7, "Log Level Fatal", Color(0x700000)),
        ColorItem(8, "PID FG", Color(0x0000FF)),
        ColorItem(9, "TID FG", Color(0x0000FF)),
        ColorItem(10, "Tag FG", Color(0x0000FF)),
        ColorItem(11, "Highlight FG", Color(0xFFFFFF)),
        ColorItem(13, "LineNum FG", Color(0x333333)),
        ColorItem(19, "NumLogSeparator BG", Color(0xFFFFFF)),
        ColorItem(20, "Bookmark Selected BG", Color(0xD0D0DF)),
        ColorItem(21, "LineNum Bookmark Selected BG", Color(0xC0C0CF)),
        ColorItem(22, "LineNum Bookmark BG", Color(0xE0E0EF)),
        ColorItem(23, "LineNum Selected BG", Color(0xC0C0C0)),
        ColorItem(24, "Highlight BG", Color(0x3030B0).alpha(0.8)),
        ColorItem(26, "Filtered 1 FG", Color(0xFFFFFF)),
        ColorItem(27, "Filtered 2 FG", Color(0xFFFFFF)),
        ColorItem(28, "Filtered 3 FG", Color(0xFFFFFF)),
        ColorItem(29, "Filtered 4 FG", Color(0xFFFFFF)),
        ColorItem(30, "Filtered 5 FG", Color(0xFFFFFF)),
        ColorItem(31, "Filtered 6 FG", Color(0xFFFFFF)),
        ColorItem(32, "Filtered 7 FG", Color(0xFFFFFF)),
        ColorItem(33, "Filtered 8 FG", Color(0xFFFFFF)),
        ColorItem(34, "Filtered 9 FG", Color(0xFFFFFF)),
        ColorItem(35, "Filtered 1 BG", Color(0x2070C0)),
        ColorItem(36, "Filtered 2 BG", Color(0xE07020)),
        ColorItem(37, "Filtered 3 BG", Color(0x10C050)),
        ColorItem(38, "Filtered 4 BG", Color(0xB09020)),
        ColorItem(39, "Filtered 5 BG", Color(0xB02020)),
        ColorItem(40, "Filtered 6 BG", Color(0x2020B0)),
        ColorItem(41, "Filtered 7 BG", Color(0xA050C0)),
        ColorItem(42, "Filtered 8 BG", Color(0x2050A0)),
        ColorItem(43, "Filtered 9 BG", Color(0x707020)),
        ColorItem(12, "Search FG", Color(0xFFFFFF)),
        ColorItem(25, "Search BG", Color(0x3030B0)),
        ColorItem(44, "Filter ComboBox Include Text", Color(0xFFFFFF)),
        ColorItem(45, "Filter ComboBox Exclude Text", Color(0xFFA0A0)),
        ColorItem(46, "Filter ComboBox Separator", Color(0x00FF00)),
    )

    private val colorArrayDark = arrayOf(
        ColorItem(0, "Filtered FG", Color(0x000000)),
        ColorItem(14, "Selected BG", Color(0x3A3D41)),
        ColorItem(15, "Filtered BG", Color(0x00A000)),
        ColorItem(16, "Log BG", Color(0x151515)),
        ColorItem(17, "LineNum BG", Color(0x151515)),
        ColorItem(18, "Bookmark BG", Color(0x501010)),
        ColorItem(1, "Log Level None", Color(0xF0F0F0)),
        ColorItem(2, "Log Level Verbose", Color(0xF0F0F0)),
        ColorItem(3, "Log Level Debug", Color(0x6C9876)),
        ColorItem(4, "Log Level Info", Color(0x5084C4)),
        ColorItem(5, "Log Level Warning", Color(0xCB8742)),
        ColorItem(6, "Log Level Error", Color(0xCD6C79)),
        ColorItem(7, "Log Level Fatal", Color(0xED3030)),
        ColorItem(8, "PID FG", Color(0xFFFFCC)),
        ColorItem(9, "TID FG", Color(0xFFCCFF)),
        ColorItem(10, "Tag FG", Color(0xCCFFFF)),
        ColorItem(11, "Highlight FG", Color(0x000000)),
        ColorItem(13, "LineNum FG", Color(0xF0F0F0)),
        ColorItem(19, "NumLogSeparator BG", Color(0xA0A0A0)),
        ColorItem(20, "Bookmark Selected BG", Color(0x503030)),
        ColorItem(21, "LineNum Bookmark Selected BG", Color(0x503030)),
        ColorItem(22, "LineNum Bookmark BG", Color(0x301010)),
        ColorItem(23, "LineNum Selected BG", Color(0x3A3D41)),
        ColorItem(24, "Highlight BG", Color(0xB0B0B0)),
        ColorItem(26, "Filtered 1 FG", Color(0x000000)),
        ColorItem(27, "Filtered 2 FG", Color(0x000000)),
        ColorItem(28, "Filtered 3 FG", Color(0x000000)),
        ColorItem(29, "Filtered 4 FG", Color(0x000000)),
        ColorItem(30, "Filtered 5 FG", Color(0x000000)),
        ColorItem(31, "Filtered 6 FG", Color(0x000000)),
        ColorItem(32, "Filtered 7 FG", Color(0x000000)),
        ColorItem(33, "Filtered 8 FG", Color(0x000000)),
        ColorItem(34, "Filtered 9 FG", Color(0x000000)),
        ColorItem(35, "Filtered 1 BG", Color(0xE06000)),
        ColorItem(36, "Filtered 2 BG", Color(0x0090E0)),
        ColorItem(37, "Filtered 3 BG", Color(0xA0A000)),
        ColorItem(38, "Filtered 4 BG", Color(0xF070A0)),
        ColorItem(39, "Filtered 5 BG", Color(0xE0E0E0)),
        ColorItem(40, "Filtered 6 BG", Color(0x00C060)),
        ColorItem(41, "Filtered 7 BG", Color(0x20B0A0)),
        ColorItem(42, "Filtered 8 BG", Color(0x9050E0)),
        ColorItem(43, "Filtered 9 BG", Color(0xC0C060)),
        ColorItem(12, "Search FG", Color(0x000000)),
        ColorItem(25, "Search BG", Color(0xB0B0B0)),
        ColorItem(44, "Filter ComboBox Include Text", Color(0x46494B)),
        ColorItem(45, "Filter ComboBox Exclude Text", Color(0xAA5050)),
        ColorItem(46, "Filter ComboBox Separator", Color(0x007700)),
    )

    var filteredFG: Color = DEFAULT_COLOR
    var filteredBG: Color = DEFAULT_COLOR
    var pidFG: Color = DEFAULT_COLOR
    var tidFG: Color = DEFAULT_COLOR
    var tagFG: Color = DEFAULT_COLOR
    var highlightFG: Color = DEFAULT_COLOR
    var searchFG: Color = DEFAULT_COLOR
    var selectedBG: Color = DEFAULT_COLOR
    var logBG: Color = DEFAULT_COLOR
    var lineNumBG: Color = DEFAULT_COLOR
    var bookmarkBG: Color = DEFAULT_COLOR
    var logLevelNone: Color = DEFAULT_COLOR
    var logLevelVerbose: Color = DEFAULT_COLOR
    var logLevelDebug: Color = DEFAULT_COLOR
    var logLevelInfo: Color = DEFAULT_COLOR
    var logLevelWarning: Color = DEFAULT_COLOR
    var logLevelError: Color = DEFAULT_COLOR
    var logLevelFatal: Color = DEFAULT_COLOR
    var lineNumFG: Color = DEFAULT_COLOR
    var numLogSeparatorBG: Color = DEFAULT_COLOR
    var bookmarkSelectedBG: Color = DEFAULT_COLOR
    var numBookmarkSelectedBG: Color = DEFAULT_COLOR
    var numBookmarkBG: Color = DEFAULT_COLOR
    var numSelectedBG: Color = DEFAULT_COLOR
    var highlightBG: Color = DEFAULT_COLOR
    var searchBG: Color = DEFAULT_COLOR
    var filteredFGs = arrayOf(
        Color(0xFFFFFF),
        Color(0xFFFFFF),
        Color(0xFFFFFF),
        Color(0xFFFFFF),
        Color(0xFFFFFF),
        Color(0xFFFFFF),
        Color(0xFFFFFF),
        Color(0xFFFFFF),
        Color(0xFFFFFF),
        Color(0xFFFFFF),
    )

    var filteredBGs = arrayOf(
        Color(0x20B020),
        Color(0x2070C0),
        Color(0xE07020),
        Color(0x10C050),
        Color(0xB09020),
        Color(0xB02020),
        Color(0x2020B0),
        Color(0xA050C0),
        Color(0x2050A0),
        Color(0x707020),
    )

    var filterStyleInclude: Color = DEFAULT_COLOR
    var filterStyleExclude: Color = DEFAULT_COLOR
    var filterStyleSeparator: Color = DEFAULT_COLOR

    override fun onThemeChanged(theme: Theme, properties: Hashtable<Any, Any>) {
        updateColorScheme(if (Theme.isDark(theme)) colorArrayDark else colorArrayLight)
    }

    private fun updateColorScheme(colorArray: Array<ColorItem>) {
        filteredFG = colorArray[TableColorIdx.FILTERED_FG.value].color
        selectedBG = colorArray[TableColorIdx.SELECTED_BG.value].color
        filteredBG = colorArray[TableColorIdx.FILTERED_BG.value].color
        logBG = colorArray[TableColorIdx.LOG_BG.value].color
        lineNumBG = colorArray[TableColorIdx.LINE_NUM_BG.value].color
        bookmarkBG = colorArray[TableColorIdx.BOOKMARK_BG.value].color
        logLevelNone = colorArray[TableColorIdx.LOG_LEVEL_NONE.value].color
        logLevelVerbose = colorArray[TableColorIdx.LOG_LEVEL_VERBOSE.value].color
        logLevelDebug = colorArray[TableColorIdx.LOG_LEVEL_DEBUG.value].color
        logLevelInfo = colorArray[TableColorIdx.LOG_LEVEL_INFO.value].color
        logLevelWarning = colorArray[TableColorIdx.LOG_LEVEL_WARNING.value].color
        logLevelError = colorArray[TableColorIdx.LOG_LEVEL_ERROR.value].color
        logLevelFatal = colorArray[TableColorIdx.LOG_LEVEL_FATAL.value].color
        pidFG = colorArray[TableColorIdx.PID_FG.value].color
        tidFG = colorArray[TableColorIdx.TID_FG.value].color
        tagFG = colorArray[TableColorIdx.TAG_FG.value].color
        highlightFG = colorArray[TableColorIdx.HIGHLIGHT_FG.value].color
        searchFG = colorArray[TableColorIdx.SEARCH_FG.value].color
        lineNumFG = colorArray[TableColorIdx.LINE_NUM_FG.value].color
        numLogSeparatorBG = colorArray[TableColorIdx.NUM_LOG_SEPARATOR_BG.value].color
        bookmarkSelectedBG = colorArray[TableColorIdx.BOOKMARK_SELECTED_BG.value].color
        numBookmarkSelectedBG = colorArray[TableColorIdx.NUM_BOOKMARK_SELECTED_BG.value].color
        numBookmarkBG = colorArray[TableColorIdx.NUM_BOOKMARK_BG.value].color
        numSelectedBG = colorArray[TableColorIdx.NUM_SELECTED_BG.value].color
        highlightBG = colorArray[TableColorIdx.HIGHLIGHT_BG.value].color
        searchBG = colorArray[TableColorIdx.SEARCH_BG.value].color
        filterStyleInclude = colorArray[TableColorIdx.FILER_COMBO_BOX_INCLUDE_BG.value].color
        filterStyleExclude = colorArray[TableColorIdx.FILER_COMBO_BOX_EXCLUDE_BG.value].color
        filterStyleSeparator = colorArray[TableColorIdx.FILER_COMBO_BOX_SEPARATOR_BG.value].color

        for (idx in filteredFGs.indices) {
            if (idx == 0) {
                filteredFGs[idx] = filteredFG
                filteredBGs[idx] = filteredBG
            } else {
                filteredFGs[idx] = colorArray[TableColorIdx.FILTERED_START_FG.value + idx - 1].color
                filteredBGs[idx] = colorArray[TableColorIdx.FILTERED_START_BG.value + idx - 1].color
            }
        }
    }
}

object Menu {
    const val MENU_ITEM_ICON_SIZE = 16
}

fun interface ThemeAware {
    fun onThemeChanged(theme: Theme, properties: Hashtable<Any, Any>)
}