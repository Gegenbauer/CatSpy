package me.gegenbauer.catspy.iconset

import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.extras.FlatSVGUtils
import com.github.weisj.darklaf.properties.icons.IconLoader
import java.awt.Image
import javax.swing.Icon

@Suppress("UNCHECKED_CAST")
fun <T : Icon> loadIcon(img: String): T {
    return if (img.endsWith(".svg")) {
        FlatSVGIcon(img) as T
    } else {
        IconLoader.get().getIcon(img) as T
    }
}

@Suppress("UNCHECKED_CAST")
fun <T : Icon> loadIcon(img: String, w: Int, h: Int): T {
    return if (img.endsWith(".svg")) {
        FlatSVGIcon(img, w, h) as T
    } else {
        IconLoader.get().getIcon(img, w, h) as T
    }
}

val appIcons: List<Image> = FlatSVGUtils.createWindowIconImages("/logo/logo.svg")