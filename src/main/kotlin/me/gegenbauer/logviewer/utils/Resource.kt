package me.gegenbauer.logviewer.utils

import java.io.File
import java.net.URL
import java.util.*

fun String.appendPath(path: String): String {
    return "$this${File.separator}$path"
}

inline fun <reified T> getFile(filePath: String): T = when (T::class) {
    String::class -> {
        filePath as T
    }

    File::class -> {
        File(filePath) as T
    }

    URL::class -> {
        File(filePath).toURI().toURL() as T
    }

    else -> {
        throw IllegalArgumentException("Unsupported type")
    }
}


const val IMAGE_RES_DIR = "images"

inline fun <reified T> getImageFile(img: String): T {
    return getFile("$IMAGE_RES_DIR${File.separator}$img")
}

fun getIconFromFile(icon: String): URL {
    return getImageFile(icon)
}


const val STRING_RES_DIR = "strings"

inline fun <reified T> getStringFile(locale: Locale): T {
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
    return getFile(STRING_RES_DIR.appendPath(filename))
}

const val THEME_RES_DIR = "themes"

inline fun <reified T> getThemeFile(theme: String): T {
    return getFile(THEME_RES_DIR.appendPath(theme))
}