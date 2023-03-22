package me.gegenbauer.logviewer.configuration

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import me.gegenbauer.logviewer.log.GLog
import me.gegenbauer.logviewer.utils.userDir
import java.io.File

object UIConfManager {
    private const val TAG = "UIConfigurationManager"
    private const val UI_FILENAME = "ui.json"

    val uiConf: UIConf by lazy { loadUI() }
    private val uiFile = File(userDir, UI_FILENAME)

    init {
        if (!uiFile.exists()) {
            uiFile.createNewFile()
            uiFile.writeText(Gson().toJson(UIConf()))
        }
    }

    private fun loadUI(): UIConf {
        return JsonReader(uiFile.reader()).use {
            Gson().fromJson<UIConf?>(it, UIConf::class.java).apply {
                GLog.d(TAG, "[loadUI] $this")
            }
        }
    }

    fun saveUI() {
        GLog.d(TAG, "[saveUI] $uiConf")
        uiFile.writeText(Gson().toJson(uiConf))
    }
}