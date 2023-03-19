package me.gegenbauer.logviewer.strings

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import me.gegenbauer.logviewer.utils.getStringFile
import java.io.InputStreamReader
import java.util.Locale

val locale: Locale = Locale.CHINA

val STRINGS = StringParser().parse(locale)

class StringParser {

    fun parse(locale: Locale): Strings {
        val inStream = getStringFile(locale)
        JsonReader(InputStreamReader(inStream)).use {
            return Gson().fromJson(it, Strings::class.java)
        }
    }

    companion object {
        private const val TAG = "StringParser"
    }
}