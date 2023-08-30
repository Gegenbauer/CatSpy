package me.gegenbauer.catspy.configuration

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.filesDir
import java.io.File

object UIConfManager {
    private const val TAG = "UIConfigurationManager"
    private const val UI_FILENAME = "ui.json"

    val uiConf: UIConf by lazy { loadUI() }
    private val uiFile = File(filesDir, UI_FILENAME)

    init {
        if (!uiFile.exists()) {
            uiFile.createNewFile()
            uiFile.writeText(Gson().toJson(UIConf()))
        }
    }

    private fun loadUI(): UIConf {
        return JsonReader(uiFile.reader()).use {
            Gson().fromJson<UIConf?>(it, UIConf::class.java).apply {
                GLog.i(TAG, "[loadUI] $this")
            }
        }
    }

    fun saveUI() {
        GLog.i(TAG, "[saveUI] $uiConf")
        uiFile.writeText(Gson().toJson(uiConf))
    }
}