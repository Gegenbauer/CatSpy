package me.gegenbauer.logviewer.configuration

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import me.gegenbauer.logviewer.utils.userDir
import java.io.File

object UIConfigurationManager {
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
            Gson().fromJson(it, UI::class.java)
        }
    }

    fun saveUI() {
        uiFile.writeText(Gson().toJson(ui))
    }
}