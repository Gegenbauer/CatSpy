package me.gegenbauer.logviewer.utils

import me.gegenbauer.logviewer.Main
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.*

fun String.appendPath(path: String): String {
    return "$this${File.separator}$path"
}

fun loadResource(resourcePath: String): InputStream {
    val contextClassLoader = Thread.currentThread().contextClassLoader
    val resource = contextClassLoader.getResourceAsStream(resourcePath)
    return requireNotNull(resource) { "Resource $resourcePath not found" }
}

private fun resources(relativePath: String): URL {
    val classLoader = Main::class.java.classLoader
    val resource = classLoader.getResource(relativePath)
    return resource ?: throw IllegalArgumentException("Resource not found")
}

private const val IMAGE_RES_DIR = "images"

fun getImageFile(img: String): URL {
    return resources(IMAGE_RES_DIR.appendPath(img))
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
    return loadResource(STRING_RES_DIR.appendPath(filename))
}