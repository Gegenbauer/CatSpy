package me.gegenbauer.catspy.strings

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import me.gegenbauer.catspy.common.Resources.loadResourceAsStream
import me.gegenbauer.catspy.file.appendPath
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

val locale: Locale = Locale.ENGLISH

val STRINGS = StringParser().parse(locale)

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
        private const val TAG = "StringParser"
        private const val STRING_RES_DIR = "strings"
    }
}