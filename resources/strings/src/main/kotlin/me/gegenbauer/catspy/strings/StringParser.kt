package me.gegenbauer.catspy.strings

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import me.gegenbauer.catspy.common.Resources.loadResourceAsStream
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.java.ext.replaceNullStringProperties
import java.io.InputStreamReader

val STRINGS: Strings
    get() = StringResourceManager.strings

object StringResourceManager {
    private const val STRING_RES_DIR = "strings"

    private val DEFAULT_STRINGS = Strings().apply {
        ui = Strings.Ui()
        toolTip = Strings.ToolTip()
    }
    internal var strings: Strings = DEFAULT_STRINGS

    fun parse(locale: Locale): Strings {
        val inStream = loadResourceAsStream(STRING_RES_DIR.appendPath(locale.stringFile))
        JsonReader(InputStreamReader(inStream)).use {
            return Gson().fromJson(it, Strings::class.java)
        }
    }

    fun loadStrings() {
        strings = parse(globalLocale).apply {
            // avoid null string properties, replace them with default strings
            replaceNullStringProperties(this, DEFAULT_STRINGS)
        }
    }
}