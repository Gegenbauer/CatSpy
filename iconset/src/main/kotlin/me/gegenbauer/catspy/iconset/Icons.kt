package me.gegenbauer.catspy.iconset

import com.github.weisj.darklaf.properties.icons.IconLoader
import javax.swing.Icon

fun <T : Icon> loadIcon(img: String): T {
    return IconLoader.get().getIcon(img, true) as T
}

fun <T : Icon> loadIcon(img: String, w: Int, h: Int): T {
    return IconLoader.get().getIcon(img, w, h, true) as T
}