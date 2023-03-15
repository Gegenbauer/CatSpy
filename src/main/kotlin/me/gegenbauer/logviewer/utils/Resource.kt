package me.gegenbauer.logviewer.utils

import java.io.File
import java.net.URL
import java.util.*


fun String.appendPath(path: String): String {
    return "$this${File.separator}$path"
}

private val RES_ROOT_DIR = "src".appendPath("main").appendPath("resources")

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


val IMAGE_RES_DIR = RES_ROOT_DIR.appendPath("images")

inline fun <reified T> getImageFile(img: String): T {
    return getFile(IMAGE_RES_DIR.appendPath(img))
}

val STRING_RES_DIR = RES_ROOT_DIR.appendPath("strings")

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

val THEME_RES_DIR = RES_ROOT_DIR.appendPath("themes")

inline fun <reified T> getThemeFile(theme: String): T {
    return getFile(THEME_RES_DIR.appendPath(theme))
}