package me.gegenbauer.catspy.configuration

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.java.ext.getEnum
import me.gegenbauer.catspy.platform.filesDir
import me.gegenbauer.catspy.strings.globalLocale
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

    fun init() {
        // empty implementation, just to trigger lazy initialization
    }

    private fun loadUI(): UIConf {
        return JsonReader(uiFile.reader()).use {
            Gson().fromJson<UIConf?>(it, UIConf::class.java).apply {
                GLog.i(TAG, "[loadUI] $this")
                globalLocale = getEnum(locale)
            }
        }
    }

    fun saveUI() {
        GLog.i(TAG, "[saveUI] $uiConf")
        uiFile.writeText(Gson().toJson(uiConf))
    }
}