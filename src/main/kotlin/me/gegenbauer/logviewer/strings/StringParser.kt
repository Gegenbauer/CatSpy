package me.gegenbauer.logviewer.strings

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import java.io.File
import java.util.Locale

val locale = Locale.CHINA

val STRINGS = StringParser().parse(locale)

class StringParser {

    fun parse(locale: Locale): Strings {
        val filePath = getStringFilePathByLocale(locale)
        JsonReader(File(filePath).reader()).use {
            return Gson().fromJson(it, Strings::class.java)
        }
    }

    companion object {
        private const val STRING_FILE_DIR = "src/main/resources/strings"

        private fun getStringFilePathByLocale(locale: Locale): String {
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
            return "$STRING_FILE_DIR${File.separator}$filename"
        }
    }
}