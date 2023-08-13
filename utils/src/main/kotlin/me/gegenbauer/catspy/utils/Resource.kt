package me.gegenbauer.catspy.utils

import me.gegenbauer.catspy.log.appendPath
import java.io.InputStream
import java.net.URL
import java.util.*

const val TAB_ICON_SIZE = 14

fun loadResourceAsStream(resourcePath: String): InputStream {
    val contextClassLoader = Thread.currentThread().contextClassLoader
    val resource = contextClassLoader.getResourceAsStream(resourcePath)
    return requireNotNull(resource) { "Resource $resourcePath not found" }
}

fun getResource(relativePath: String): URL {
    val classLoader = Thread.currentThread().contextClassLoader
    val resource = classLoader.getResource(relativePath)
    return resource ?: throw IllegalArgumentException("Resource not found")
}

private const val STRING_RES_DIR = "strings"

fun getStringFile(locale: Locale): InputStream {
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