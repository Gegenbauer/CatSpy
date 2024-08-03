package me.gegenbauer.catspy.view.color

import java.awt.Color
import java.awt.PaintContext
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.color.ColorSpace
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import java.awt.image.ColorModel

abstract class ColorWrapper: Color(0) {
    
    abstract val color: Color

    override fun getRed(): Int {
        return color.red
    }

    override fun getTransparency(): Int {
        return color.transparency
    }

    override fun createContext(
        cm: ColorModel?,
        r: Rectangle?,
        r2d: Rectangle2D?,
        xform: AffineTransform?,
        hints: RenderingHints?
    ): PaintContext {
        return color.createContext(cm, r, r2d, xform, hints)
    }

    override fun getGreen(): Int {
        return color.green
    }

    override fun getBlue(): Int {
        return color.blue
    }

    override fun getAlpha(): Int {
        return color.alpha
    }

    override fun brighter(): Color {
        return color.brighter()
    }

    override fun getRGB(): Int {
        return color.rgb
    }

    override fun darker(): Color {
        return color.darker()
    }

    override fun getRGBComponents(compArray: FloatArray?): FloatArray {
        return color.getRGBComponents(compArray)
    }

    override fun getRGBColorComponents(compArray: FloatArray?): FloatArray {
        return color.getRGBColorComponents(compArray)
    }

    override fun getComponents(compArray: FloatArray?): FloatArray {
        return color.getComponents(compArray)
    }

    override fun getColorComponents(compArray: FloatArray?): FloatArray {
        return color.getColorComponents(compArray)
    }

    override fun getComponents(cspace: ColorSpace?, compArray: FloatArray?): FloatArray {
        return color.getComponents(cspace, compArray)
    }

    override fun getColorComponents(cspace: ColorSpace?, compArray: FloatArray?): FloatArray {
        return color.getColorComponents(cspace, compArray)
    }

    override fun getColorSpace(): ColorSpace {
        return color.getColorSpace()
    }
    
    companion object {
        private val default = Color(0)
    }
}