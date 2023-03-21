package me.gegenbauer.logviewer.configuration

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.utils.userDir
import java.io.File

object UIConfigurationManager {
    private const val TAG = "UIConfigurationManager"
    private const val UI_FILENAME = "ui.json"

    val ui: UI by lazy { loadUI() }
    private val uiFile = File(userDir, UI_FILENAME)

    init {
        if (!uiFile.exists()) {
            uiFile.createNewFile()
            uiFile.writeText(Gson().toJson(UI()))
        }
    }

    private fun loadUI(): UI {
        return JsonReader(uiFile.reader()).use {
            Gson().fromJson<UI?>(it, UI::class.java).apply {
                GLog.d(TAG, "[loadUI] $this")
            }
        }
    }

    fun saveUI() {
        GLog.d(TAG, "[saveUI] $ui")
        uiFile.writeText(Gson().toJson(ui))
    }
}