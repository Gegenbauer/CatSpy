package me.gegenbauer.logviewer.manager

import java.awt.Color

class ColorManager private constructor(){
    class ColorEvent(change:Int) {
        val colorChange = change
    }

    interface ColorEventListener {
        fun colorChanged(event: ColorEvent)
    }

    companion object {
        private val instance: ColorManager = ColorManager()

        fun getInstance(): ColorManager {
            return instance
        }
    }

    private val colorEventListeners = ArrayList<ColorEventListener>()
    private val filterStyleEventListeners = ArrayList<ColorEventListener>()
    
    fun addColorEventListener(listener: ColorEventListener) {
        colorEventListeners.add(listener)
    }

    fun removeColorEventListener(listener: ColorEventListener) {
        colorEventListeners.remove(listener)
    }

    data class ColorItem(val order: Int, val name: String, var strColor: String)

    private val configManager = ConfigManager.getInstance()

    enum class TableColorType(val value: Int) {
        FULL_LOG_TABLE(0),
        FILTER_LOG_TABLE(1);

        companion object {
            fun fromInt(value: Int) = values().first { it.value == value }
        }
    }

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
        ;

        companion object {
            fun fromInt(value: Int) = values().first { it.value == value }
        }
    }

    var colorSchemeLight = arrayOf(
            "#FFFFFF",
            "#E0E0E0",
            "#20B020",
            "#FFFAFA",
            "#D0D0D0",
            "#E0E0FF",
            "#000000",
            "#000000",
            "#209000",
            "#0080DF",
            "#F07000",    // 10
            "#D00000",
            "#700000",
            "#0000FF",
            "#0000FF",
            "#0000FF",
            "#FFFFFF",
            "#333333",
            "#FFFFFF",
            "#D0D0DF",
            "#C0C0CF",   // 20
            "#E0E0EF",
            "#C0C0C0",
            "#3030B0",
            "#FFFFFF",
            "#FFFFFF",
            "#FFFFFF",
            "#FFFFFF",
            "#FFFFFF",
            "#FFFFFF",
            "#FFFFFF",   // 30
            "#FFFFFF",
            "#FFFFFF",
            "#2070C0",
            "#E07020",
            "#10C050",
            "#B09020",
            "#B02020",
            "#2020B0",
            "#A050C0",
            "#2050A0",   // 40
            "#707020",
            "#FFFFFF",
            "#3030B0",
    )

    var colorSchemeDark = arrayOf(
            "#000000",
            "#3A3D41",
            "#00A000",
            "#151515",
            "#151515",
            "#501010",
            "#F0F0F0",
            "#F0F0F0",
            "#6C9876",
            "#5084C4",
            "#CB8742",   // 10
            "#CD6C79",
            "#ED3030",
            "#FFFFCC",
            "#FFCCFF",
            "#CCFFFF",
            "#000000",
            "#F0F0F0",
            "#A0A0A0",
            "#503030",
            "#503030",   // 20
            "#301010",
            "#3A3D41",
            "#B0B0B0",
            "#000000",
            "#000000",
            "#000000",
            "#000000",
            "#000000",
            "#000000",
            "#000000",   // 30
            "#000000",
            "#000000",
            "#E06000",
            "#0090E0",
            "#A0A000",
            "#F070A0",
            "#E0E0E0",
            "#00C060",
            "#20B0A0",
            "#9050E0",   // 40
            "#C0C060",
            "#000000",
            "#B0B0B0",
    )

    // Must be declared after colorSchemeLight (internally colorSchemeLight is used)
    val fullTableColor = TableColor(TableColorType.FULL_LOG_TABLE)
    val filterTableColor = TableColor(TableColorType.FILTER_LOG_TABLE)

    inner class TableColor(val type: TableColorType) {
        var strFilteredFG = "#000000"
            set(value) {
                field = value
                filteredFG = Color.decode(value)
            }
        var filteredFG: Color = Color.decode(strFilteredFG)
            private set

        var strFilteredBG = "#000000"
            set(value) {
                field = value
                filteredBG = Color.decode(value)
            }
        var filteredBG: Color = Color.decode(strFilteredBG)
            private set

        var strPidFG = "#000000"
            set(value) {
                field = value
                pidFG = Color.decode(value)
            }
        var pidFG: Color = Color.decode(strPidFG)
            private set

        var strTidFG = "#000000"
            set(value) {
                field = value
                tidFG = Color.decode(value)
            }
        var tidFG: Color = Color.decode(strTidFG)
            private set

        var strTagFG = "#000000"
            set(value) {
                field = value
                tagFG = Color.decode(value)
            }
        var tagFG: Color = Color.decode(strTagFG)
            private set

        var strHighlightFG = "#000000"
            set(value) {
                field = value
                highlightFG = Color.decode(value)
            }
        var highlightFG: Color = Color.decode(strHighlightFG)
            private set

        var strSearchFG = "#000000"
            set(value) {
                field = value
                searchFG = Color.decode(value)
            }
        var searchFG: Color = Color.decode(strSearchFG)
            private set

        var strSelectedBG = "#000000"
            set(value) {
                field = value
                selectedBG = Color.decode(value)
            }
        var selectedBG: Color = Color.decode(strSelectedBG)
            private set

        var strLogBG = "#000000"
            set(value) {
                field = value
                logBG = Color.decode(value)
            }
        var logBG: Color = Color.decode(strLogBG)
            private set

        var strLineNumBG = "#000000"
            set(value) {
                field = value
                lineNumBG = Color.decode(value)
            }
        var lineNumBG: Color = Color.decode(strLineNumBG)
            private set

        var strBookmarkBG = "#000000"
            set(value) {
                field = value
                bookmarkBG = Color.decode(value)
            }
        var bookmarkBG: Color = Color.decode(strBookmarkBG)
            private set

        var strLogLevelNone = "#000000"
            set(value) {
                field = value
                logLevelNone = Color.decode(value)
            }
        var logLevelNone: Color = Color.decode(strLogLevelNone)
            private set

        var strLogLevelVerbose = "#000000"
            set(value) {
                field = value
                logLevelVerbose = Color.decode(value)
            }
        var logLevelVerbose: Color = Color.decode(strLogLevelVerbose)
            private set

        var strLogLevelDebug = "#000000"
            set(value) {
                field = value
                logLevelDebug = Color.decode(value)
            }
        var logLevelDebug: Color = Color.decode(strLogLevelDebug)
            private set

        var strLogLevelInfo = "#000000"
            set(value) {
                field = value
                logLevelInfo = Color.decode(value)
            }
        var logLevelInfo: Color = Color.decode(strLogLevelInfo)
            private set

        var strLogLevelWarning = "#000000"
            set(value) {
                field = value
                logLevelWarning = Color.decode(value)
            }
        var logLevelWarning: Color = Color.decode(strLogLevelWarning)
            private set

        var strLogLevelError = "#000000"
            set(value) {
                field = value
                logLevelError = Color.decode(value)
            }
        var logLevelError: Color = Color.decode(strLogLevelError)
            private set

        var strLogLevelFatal = "#000000"
            set(value) {
                field = value
                logLevelFatal = Color.decode(value)
            }
        var logLevelFatal: Color = Color.decode(strLogLevelFatal)
            private set

        var strLineNumFG = "#000000"
            set(value) {
                field = value
                lineNumFG = Color.decode(value)
            }
        var lineNumFG: Color = Color.decode(strLineNumBG)
            private set

        var strNumLogSeparatorBG = "#000000"
            set(value) {
                field = value
                numLogSeparatorBG = Color.decode(value)
            }
        var numLogSeparatorBG: Color = Color.decode(strNumLogSeparatorBG)
            private set

        var strBookmarkSelectedBG = "#000000"
            set(value) {
                field = value
                bookmarkSelectedBG = Color.decode(value)
            }
        var bookmarkSelectedBG: Color = Color.decode(strBookmarkSelectedBG)
            private set

        var strNumBookmarkSelectedBG = "#000000"
            set(value) {
                field = value
                numBookmarkSelectedBG = Color.decode(value)
            }
        var numBookmarkSelectedBG: Color = Color.decode(strNumBookmarkSelectedBG)
            private set

        var strNumBookmarkBG = "#000000"
            set(value) {
                field = value
                numBookmarkBG = Color.decode(value)
            }
        var numBookmarkBG: Color = Color.decode(strNumBookmarkBG)
            private set

        var strNumSelectedBG = "#000000"
            set(value) {
                field = value
                numSelectedBG = Color.decode(value)
            }
        var numSelectedBG: Color = Color.decode(strNumSelectedBG)
            private set

        var strHighlightBG = "#000000"
            set(value) {
                field = value
                highlightBG = Color.decode(value)
            }
        var highlightBG: Color = Color.decode(strHighlightBG)
            private set

        var strSearchBG = "#000000"
            set(value) {
                field = value
                searchBG = Color.decode(value)
            }
        var searchBG: Color = Color.decode(strSearchBG)
            private set
        
        var strFilteredFGs = arrayOf(colorSchemeLight[0],
                colorSchemeLight[24],
                colorSchemeLight[25],
                colorSchemeLight[26],
                colorSchemeLight[27],
                colorSchemeLight[28],
                colorSchemeLight[29],
                colorSchemeLight[30],
                colorSchemeLight[31],
                colorSchemeLight[32],
        )

        var strFilteredBGs = arrayOf(colorSchemeLight[2],
                colorSchemeLight[33],
                colorSchemeLight[34],
                colorSchemeLight[35],
                colorSchemeLight[36],
                colorSchemeLight[37],
                colorSchemeLight[38],
                colorSchemeLight[39],
                colorSchemeLight[40],
                colorSchemeLight[41],
        )

        var colorArray = arrayOf(
                ColorItem(0, "Filtered FG", colorSchemeLight[0]),
                ColorItem(14, "Selected BG", colorSchemeLight[1]),
                ColorItem(15, "Filtered BG", colorSchemeLight[2]),
                ColorItem(16, "Log BG", colorSchemeLight[3]),
                ColorItem(17, "LineNum BG", colorSchemeLight[4]),
                ColorItem(18, "Bookmark BG", colorSchemeLight[5]),
                ColorItem(1, "Log Level None", colorSchemeLight[6]),
                ColorItem(2, "Log Level Verbose", colorSchemeLight[7]),
                ColorItem(3, "Log Level Debug", colorSchemeLight[8]),
                ColorItem(4, "Log Level Info", colorSchemeLight[9]),
                ColorItem(5, "Log Level Warning", colorSchemeLight[10]),
                ColorItem(6, "Log Level Error", colorSchemeLight[11]),
                ColorItem(7, "Log Level Fatal", colorSchemeLight[12]),
                ColorItem(8, "PID FG", colorSchemeLight[13]),
                ColorItem(9, "TID FG", colorSchemeLight[14]),
                ColorItem(10, "Tag FG", colorSchemeLight[15]),
                ColorItem(11, "Highlight FG", colorSchemeLight[16]),
                ColorItem(13, "LineNum FG", colorSchemeLight[17]),
                ColorItem(19, "NumLogSeparator BG", colorSchemeLight[18]),
                ColorItem(20, "Bookmark Selected BG", colorSchemeLight[19]),
                ColorItem(21, "LineNum Bookmark Selected BG", colorSchemeLight[20]),
                ColorItem(22, "LineNum Bookmark BG", colorSchemeLight[21]),
                ColorItem(23, "LineNum Selected BG", colorSchemeLight[22]),
                ColorItem(24, "Highlight BG", colorSchemeLight[23]),
                ColorItem(26, "Filtered 1 FG", colorSchemeLight[24]),
                ColorItem(27, "Filtered 2 FG", colorSchemeLight[25]),
                ColorItem(28, "Filtered 3 FG", colorSchemeLight[26]),
                ColorItem(29, "Filtered 4 FG", colorSchemeLight[27]),
                ColorItem(30, "Filtered 5 FG", colorSchemeLight[28]),
                ColorItem(31, "Filtered 6 FG", colorSchemeLight[29]),
                ColorItem(32, "Filtered 7 FG", colorSchemeLight[30]),
                ColorItem(33, "Filtered 8 FG", colorSchemeLight[31]),
                ColorItem(34, "Filtered 9 FG", colorSchemeLight[32]),
                ColorItem(35, "Filtered 1 BG", colorSchemeLight[33]),
                ColorItem(36, "Filtered 2 BG", colorSchemeLight[34]),
                ColorItem(37, "Filtered 3 BG", colorSchemeLight[35]),
                ColorItem(38, "Filtered 4 BG", colorSchemeLight[36]),
                ColorItem(39, "Filtered 5 BG", colorSchemeLight[37]),
                ColorItem(40, "Filtered 6 BG", colorSchemeLight[38]),
                ColorItem(41, "Filtered 7 BG", colorSchemeLight[39]),
                ColorItem(42, "Filtered 8 BG", colorSchemeLight[40]),
                ColorItem(43, "Filtered 9 BG", colorSchemeLight[41]),
                ColorItem(12, "Search FG", colorSchemeLight[42]),
                ColorItem(25, "Search BG", colorSchemeLight[43]),
        )

        fun getConfig() {
            for (idx in colorArray.indices) {
                val item = configManager.getItem("${ConfigManager.ITEM_COLOR_MANAGER}${type}_$idx")
                if (item != null) {
                    colorArray[idx].strColor = item
                }
            }
        }

        fun putConfig() {
            for (idx in colorArray.indices) {
                configManager.setItem("${ConfigManager.ITEM_COLOR_MANAGER}${type}_$idx", colorArray[idx].strColor)
            }
        }

        fun applyColor() {
            strFilteredFG = colorArray[TableColorIdx.FILTERED_FG.value].strColor
            strSelectedBG = colorArray[TableColorIdx.SELECTED_BG.value].strColor
            strFilteredBG = colorArray[TableColorIdx.FILTERED_BG.value].strColor
            strLogBG = colorArray[TableColorIdx.LOG_BG.value].strColor
            strLineNumBG = colorArray[TableColorIdx.LINE_NUM_BG.value].strColor
            strBookmarkBG = colorArray[TableColorIdx.BOOKMARK_BG.value].strColor
            strLogLevelNone = colorArray[TableColorIdx.LOG_LEVEL_NONE.value].strColor
            strLogLevelVerbose = colorArray[TableColorIdx.LOG_LEVEL_VERBOSE.value].strColor
            strLogLevelDebug = colorArray[TableColorIdx.LOG_LEVEL_DEBUG.value].strColor
            strLogLevelInfo = colorArray[TableColorIdx.LOG_LEVEL_INFO.value].strColor
            strLogLevelWarning = colorArray[TableColorIdx.LOG_LEVEL_WARNING.value].strColor
            strLogLevelError = colorArray[TableColorIdx.LOG_LEVEL_ERROR.value].strColor
            strLogLevelFatal = colorArray[TableColorIdx.LOG_LEVEL_FATAL.value].strColor
            strPidFG = colorArray[TableColorIdx.PID_FG.value].strColor
            strTidFG = colorArray[TableColorIdx.TID_FG.value].strColor
            strTagFG = colorArray[TableColorIdx.TAG_FG.value].strColor
            strHighlightFG = colorArray[TableColorIdx.HIGHLIGHT_FG.value].strColor
            strSearchFG = colorArray[TableColorIdx.SEARCH_FG.value].strColor
            strLineNumFG = colorArray[TableColorIdx.LINE_NUM_FG.value].strColor
            strNumLogSeparatorBG = colorArray[TableColorIdx.NUM_LOG_SEPARATOR_BG.value].strColor
            strBookmarkSelectedBG = colorArray[TableColorIdx.BOOKMARK_SELECTED_BG.value].strColor
            strNumBookmarkSelectedBG = colorArray[TableColorIdx.NUM_BOOKMARK_SELECTED_BG.value].strColor
            strNumBookmarkBG = colorArray[TableColorIdx.NUM_BOOKMARK_BG.value].strColor
            strNumSelectedBG = colorArray[TableColorIdx.NUM_SELECTED_BG.value].strColor
            strHighlightBG = colorArray[TableColorIdx.HIGHLIGHT_BG.value].strColor
            strSearchBG = colorArray[TableColorIdx.SEARCH_BG.value].strColor

            for (idx in strFilteredFGs.indices) {
                if (idx == 0) {
                    strFilteredFGs[idx] = strFilteredFG
                    strFilteredBGs[idx] = strFilteredBG
                }
                else {
                    strFilteredFGs[idx] = colorArray[TableColorIdx.FILTERED_START_FG.value + idx - 1].strColor
                    strFilteredBGs[idx] = colorArray[TableColorIdx.FILTERED_START_BG.value + idx - 1].strColor
                }
            }

            for (listener in colorEventListeners) {
                listener.colorChanged(ColorEvent(0))
            }
        }
    }

    var filterColorSchemeLight = arrayOf(
            "#FFFFFF",
            "#FFA0A0",
            "#00FF00",
    )

    var filterColorSchemeDark = arrayOf(
            "#46494B",
            "#AA5050",
            "#007700",
    )

    var filterStyle = arrayOf(
            ColorItem(0, "Include Text", filterColorSchemeLight[0]),
            ColorItem(1, "Exclude Text", filterColorSchemeLight[1]),
            ColorItem(2, "Separator", filterColorSchemeLight[2]),
    )

    var filterStyleInclude: Color = Color.decode(filterStyle[0].strColor)
    var filterStyleExclude: Color = Color.decode(filterStyle[1].strColor)
    var filterStyleSeparator: Color = Color.decode(filterStyle[2].strColor)

    fun applyFilterStyle() {
        filterStyleInclude = Color.decode(filterStyle[0].strColor)
        filterStyleExclude = Color.decode(filterStyle[1].strColor)
        filterStyleSeparator = Color.decode(filterStyle[2].strColor)

        for (listener in filterStyleEventListeners) {
            listener.colorChanged(ColorEvent(0))
        }
    }

    fun getConfigFilterStyle() {
        for (idx in filterStyle.indices) {
            val item = configManager.getItem(ConfigManager.ITEM_COLOR_FILTER_STYLE + idx)
            if (item != null) {
                filterStyle[idx].strColor = item
            }
        }
    }

    fun putConfigFilterStyle() {
        for (idx in filterStyle.indices) {
            configManager.setItem(ConfigManager.ITEM_COLOR_FILTER_STYLE + idx, filterStyle[idx].strColor)
        }
    }

    fun addFilterStyleEventListener(listener: ColorEventListener) {
        filterStyleEventListeners.add(listener)
    }

    fun removeFilterStyleEventListener(listener: ColorEventListener) {
        filterStyleEventListeners.remove(listener)
    }
}

