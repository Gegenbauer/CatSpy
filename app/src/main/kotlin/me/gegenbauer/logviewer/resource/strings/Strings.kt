package me.gegenbauer.logviewer.resource.strings

import com.google.gson.annotations.SerializedName
import java.util.Locale

// TODO use tool to automatically generate this file
data class Strings(
    @SerializedName("UI")
    val ui: StringUI,
    @SerializedName("TOOL_TIP")
    val toolTip: StringToolTip
)

inline val StringUI.helpText: String
    get() = when (locale) {
        Locale.ENGLISH -> {
            HelpText.textEn
        }
        Locale.KOREAN -> {
            HelpText.textKo
        }
        else -> {
            HelpText.textCn
        }
    }

inline val StringUI.version: String
    get() = "1.0.0"

inline val StringUI.app: String
    get() = "CatSpy"


data class StringUI(
    @SerializedName("FILE")
    val file: String,

    @SerializedName("OPEN")
    val open: String,

    @SerializedName("FOLLOW")
    val follow: String,

    @SerializedName("OPEN_FILES")
    val openFiles: String,

    @SerializedName("APPEND_FILES")
    val appendFiles: String,

    @SerializedName("OPEN_RECENTS")
    val openRecents: String,

    @SerializedName("CLOSE")
    val close: String,

    @SerializedName("EXIT")
    val exit: String,

    @SerializedName("SETTING")
    val setting: String,

    @SerializedName("ADB")
    val adb: String,

    @SerializedName("FONT")
    val font: String,

    @SerializedName("LOG")
    val log: String,

    @SerializedName("START")
    val start: String,

    @SerializedName("STOP")
    val stop: String,

    @SerializedName("PAUSE")
    val pause: String,

    @SerializedName("CLEAR_VIEWS")
    val clearViews: String,

    @SerializedName("ROTATION")
    val rotation: String,

    @SerializedName("FIRST")
    val first: String,

    @SerializedName("LAST")
    val last: String,

    @SerializedName("BOLD")
    val bold: String,

    @SerializedName("HIDE")
    val hide: String,

    @SerializedName("TAG")
    val tag: String,

    @SerializedName("PID")
    val pid: String,

    @SerializedName("TID")
    val tid: String,

    @SerializedName("FILTER")
    val filter: String,

    @SerializedName("WINDOWED_MODE")
    val windowedMode: String,

    @SerializedName("VIEW")
    val view: String,

    @SerializedName("VIEW_FULL")
    val viewFull: String,

    @SerializedName("HELP")
    val help: String,

    @SerializedName("ABOUT")
    val about: String,

    @SerializedName("BOOKMARKS")
    val bookmarks: String,

    @SerializedName("FULL")
    val full: String,

    @SerializedName("INCREMENTAL")
    val incremental: String,

    @SerializedName("HIGHLIGHT")
    val highlight: String,

    @SerializedName("LOGLEVEL")
    val logLevel: String,

    @SerializedName("DEBUG")
    val debug: String,

    @SerializedName("SCROLL_BACK")
    val scrollBack: String,

    @SerializedName("CLEAR_SAVE")
    val clearSave: String,

    @SerializedName("LOGFILE")
    val logFile: String,

    @SerializedName("CONNECT")
    val connect: String,

    @SerializedName("REFRESH")
    val refresh: String,

    @SerializedName("DISCONNECT")
    val disconnect: String,

    @SerializedName("APPLY")
    val apply: String,

    @SerializedName("SCROLL_BACK_LINES")
    val scrollBackLines: String,

    @SerializedName("SPLIT_FILE")
    val splitFile: String,

    @SerializedName("COLOR")
    val color: String,

    @SerializedName("FILTERS")
    val filters: String,

    @SerializedName("KEEP")
    val keep: String,

    @SerializedName("OK")
    val ok: String,

    @SerializedName("CANCEL")
    val cancel: String,

    @SerializedName("NEW")
    val new: String,

    @SerializedName("COPY")
    val copy: String,

    @SerializedName("EDIT")
    val edit: String,

    @SerializedName("DELETE")
    val delete: String,

    @SerializedName("SAVE")
    val save: String,

    @SerializedName("SELECT")
    val select: String,

    @SerializedName("ADB_PATH")
    val adbPath: String,

    @SerializedName("LOG_PATH")
    val logPath: String,

    @SerializedName("SIZE")
    val size: String,

    @SerializedName("CONNECTED")
    val connected: String,

    @SerializedName("NOT_CONNECTED")
    val notConnected: String,

    @SerializedName("APPEND")
    val append: String,

    @SerializedName("MSG_SELECT_OPEN_MODE")
    val msgSelectOpenMode: String,

    @SerializedName("NONE")
    val none: String,

    @SerializedName("ADD_FILTER")
    val addFilter: String,

    @SerializedName("ADD_CMD")
    val addCmd: String,

    @SerializedName("COMMANDS")
    val commands: String,

    @SerializedName("RETRY_ADB")
    val retryAdb: String,

    @SerializedName("FILTER_STYLE")
    val filterStyle: String,

    @SerializedName("LOOK_AND_FEEL")
    val lookAndFeel: String,

    @SerializedName("FULL_LOG_TABLE")
    val fullLogTable: String,

    @SerializedName("FILTER_LOG_TABLE")
    val filterLogTable: String,

    @SerializedName("BUILT_IN_SCHEMES")
    val builtInSchemes: String,

    @SerializedName("LIGHT")
    val light: String,

    @SerializedName("DARK")
    val dark: String,

    @SerializedName("APPEARANCE")
    val appearance: String,

    @SerializedName("OPTIONS")
    val options: String,

    @SerializedName("LOG_CMD")
    val logCmd: String,

    @SerializedName("NOT_FOUND")
    val notFound: String,

    @SerializedName("ADD_INCLUDE")
    val addInclude: String,

    @SerializedName("ADD_EXCLUDE")
    val addExclude: String,

    @SerializedName("ADD_SEARCH")
    val addSearch: String,

    @SerializedName("SET_SEARCH")
    val setSearch: String,

    @SerializedName("SEARCH")
    val search: String,

    @SerializedName("CMD")
    val cmd: String,

    @SerializedName("THEME")
    val theme: String,

    @SerializedName("DARCULA")
    val darcula: String,

    @SerializedName("INTELLIJ")
    val intelliJ: String,
)

class StringToolTip(
    @SerializedName("START_BTN")
    val startBtn: String,

    @SerializedName("PAUSE_BTN")
    val pauseBtn: String,

    @SerializedName("STOP_BTN")
    val stopBtn: String,

    @SerializedName("CLEAR_BTN")
    val clearBtn: String,

    @SerializedName("SAVE_BTN")
    val saveBtn: String,

    @SerializedName("DEVICES_COMBO")
    val devicesCombo: String,

    @SerializedName("CONNECT_BTN")
    val connectBtn: String,

    @SerializedName("REFRESH_BTN")
    val refreshBtn: String,

    @SerializedName("DISCONNECT_BTN")
    val disconnectBtn: String,

    @SerializedName("SCROLL_BACK_TF")
    val scrollBackTf: String,

    @SerializedName("SCROLL_BACK_SPLIT_CHK")
    val scrollBackSplitChk: String,

    @SerializedName("SCROLL_BACK_APPLY_BTN")
    val scrollBackApplyBtn: String,

    @SerializedName("SCROLL_BACK_KEEP_TOGGLE")
    val scrollBackKeepToggle: String,

    @SerializedName("ROTATION_BTN")
    val rotationBtn: String,

    @SerializedName("CASE_TOGGLE")
    val caseToggle: String,

    @SerializedName("FILTER_LIST_BTN")
    val filterListBtn: String,

    @SerializedName("LOG_TOGGLE")
    val logToggle: String,

    @SerializedName("LOG_COMBO")
    val logCombo: String,

    @SerializedName("TAG_TOGGLE")
    val tagToggle: String,

    @SerializedName("TAG_COMBO")
    val tagCombo: String,

    @SerializedName("PID_TOGGLE")
    val pidToggle: String,

    @SerializedName("PID_COMBO")
    val pidCombo: String,

    @SerializedName("TID_TOGGLE")
    val tidToggle: String,

    @SerializedName("TID_COMBO")
    val tidCombo: String,

    @SerializedName("BOLD_TOGGLE")
    val boldToggle: String,

    @SerializedName("BOLD_COMBO")
    val boldCombo: String,

    @SerializedName("VIEW_FIRST_BTN")
    val viewFirstBtn: String,

    @SerializedName("VIEW_LAST_BTN")
    val viewLastBtn: String,

    @SerializedName("VIEW_PID_TOGGLE")
    val viewPidToggle: String,

    @SerializedName("VIEW_TID_TOGGLE")
    val viewTidToggle: String,

    @SerializedName("VIEW_TAG_TOGGLE")
    val viewTagToggle: String,

    @SerializedName("VIEW_FULL_TOGGLE")
    val viewFullToggle: String,

    @SerializedName("VIEW_BOOKMARKS_TOGGLE")
    val viewBookmarksToggle: String,

    @SerializedName("VIEW_WINDOWED_MODE_BTN")
    val viewWindowedModeBtn: String,

    @SerializedName("SAVED_FILE_TF")
    val savedFileTf: String,

    @SerializedName("ADD_FILTER_BTN")
    val addFilterBtn: String,

    @SerializedName("ADD_CMD_BTN")
    val addCmdBtn: String,

    @SerializedName("CMD_LIST_BTN")
    val cmdListBtn: String,

    @SerializedName("RETRY_ADB_TOGGLE")
    val retryAdbToggle: String,

    @SerializedName("START_FOLLOW_BTN")
    val startFollowBtn: String,

    @SerializedName("STOP_FOLLOW_BTN")
    val stopFollowBtn: String,

    @SerializedName("LOG_CMD_COMBO")
    val logCmdCombo: String,

    @SerializedName("SEARCH_COMBO")
    val searchCombo: String,

    @SerializedName("SEARCH_CASE_TOGGLE")
    val searchCaseToggle: String,

    @SerializedName("SEARCH_PREV_BTN")
    val searchPrevBtn: String,

    @SerializedName("SEARCH_NEXT_BTN")
    val searchNextBtn: String,

    @SerializedName("SEARCH_TARGET_LABEL")
    val searchTargetLabel: String,

    @SerializedName("SEARCH_CLOSE_BTN")
    val searchCloseBtn: String
)