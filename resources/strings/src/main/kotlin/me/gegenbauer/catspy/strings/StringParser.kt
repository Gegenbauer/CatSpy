package me.gegenbauer.catspy.strings

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import me.gegenbauer.catspy.common.Resources.loadResourceAsStream
import me.gegenbauer.catspy.file.appendPath
import me.gegenbauer.catspy.java.ext.replaceNullStringProperties
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

val DEFAULT_STRINGS = Strings().apply {
    ui = Strings.Ui()
    toolTip = Strings.ToolTip()
}

val STRINGS = StringParser().parse(Locale.getDefault()).apply {
    // avoid null string properties, replace them with default strings
    replaceNullStringProperties(this, DEFAULT_STRINGS)
}

inline val Strings.Ui.helpText: String
    get() = when (Locale.getDefault()) {
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

inline val Strings.Ui.version: String
    get() = "1.0.0"

inline val Strings.Ui.app: String
    get() = "CatSpy"

class StringParser {

    fun parse(locale: Locale): Strings {
        val inStream = getStringFile(locale)
        JsonReader(InputStreamReader(inStream)).use {
            return Gson().fromJson(it, Strings::class.java)
        }
    }

    private fun getStringFile(locale: Locale): InputStream {
        val filename = when (locale) {
            Locale.KOREAN -> {
                "ko.json"
            }

            Locale.CHINA -> {
                "zh_cn.json"
            }

            Locale.ENGLISH -> {
                "en.json"
            }

            else -> {
                "zh_cn.json"
            }
        }
        return loadResourceAsStream(STRING_RES_DIR.appendPath(filename))
    }

    companion object {
        private const val STRING_RES_DIR = "strings"
    }
}