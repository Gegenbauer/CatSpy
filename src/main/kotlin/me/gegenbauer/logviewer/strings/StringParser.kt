package me.gegenbauer.logviewer.strings

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import me.gegenbauer.logviewer.utils.getStringFile
import java.io.File
import java.util.Locale

val locale = Locale.CHINA

val STRINGS = StringParser().parse(locale)

class StringParser {

    fun parse(locale: Locale): Strings {
        JsonReader(getStringFile<File>(locale).reader()).use {
            return Gson().fromJson(it, Strings::class.java)
        }
    }
}