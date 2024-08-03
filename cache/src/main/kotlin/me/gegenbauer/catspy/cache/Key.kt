package me.gegenbauer.catspy.cache

import java.nio.charset.Charset
import java.security.MessageDigest

interface Key {

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

    companion object {
        const val STRING_CHARSET_NAME = "UTF-8"
        val CHARSET = Charset.forName(STRING_CHARSET_NAME)
    }
}