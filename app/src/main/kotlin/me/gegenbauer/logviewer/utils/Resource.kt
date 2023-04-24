package me.gegenbauer.logviewer.utils

import com.github.weisj.darklaf.properties.icons.IconLoader
import me.gegenbauer.logviewer.Main
import me.gegenbauer.logviewer.ui.iconDefaultSize
import java.io.File
import java.io.InputStream
import java.net.URL
import java.util.*
import javax.swing.Icon

fun String.appendPath(path: String): String {
    return "$this${File.separator}$path"
}

fun loadResourceAsStream(resourcePath: String): InputStream {
    val contextClassLoader = Thread.currentThread().contextClassLoader
    val resource = contextClassLoader.getResourceAsStream(resourcePath)
    return requireNotNull(resource) { "Resource $resourcePath not found" }
}

fun getResource(relativePath: String): URL {
    val classLoader = Main::class.java.classLoader
    val resource = classLoader.getResource(relativePath)
    return resource ?: throw IllegalArgumentException("Resource not found")
}

private const val IMAGE_RES_DIR = "images"

fun <T : Icon> loadIcon(img: String, themed: Boolean = false, w: Int = iconDefaultSize, h: Int = iconDefaultSize): T {
    return IconLoader.get().getIcon(IMAGE_RES_DIR.appendPath(img), w, h, themed) as T
}

fun <T : Icon> loadIconWithRealSize(img: String, themed: Boolean = false): T {
    return IconLoader.get().getIcon(IMAGE_RES_DIR.appendPath(img), themed) as T
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