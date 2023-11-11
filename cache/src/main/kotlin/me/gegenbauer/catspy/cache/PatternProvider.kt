package me.gegenbauer.catspy.cache

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextService
import java.security.MessageDigest
import java.util.regex.Pattern

class PatternProvider(size: Long = DEFAULT_CACHE_SIZE) : ContextService, LruCache<PatternKey, Pattern>(size) {

    override fun get(key: PatternKey): Pattern? {
        return super.get(key).takeIf { it != null } ?: Pattern.compile(key.pattern, key.getPatternFlag()).apply {
            CacheLog.d(TAG, "[get] miss, pattern: $key")
            put(key, this)
        }
    }

    override fun onContextDestroyed(context: Context) {
        clearMemory()
    }

    companion object {
        val EMPTY_PATTERN: Pattern = Pattern.compile("")
        private const val TAG = "PatternProvider"
        private const val DEFAULT_CACHE_SIZE = 1000L

        fun String.toPatternKey(matchCase: Boolean = false) = PatternKey(this, matchCase)
    }

}

inline val Pattern.isEmpty: Boolean
    get() = this == PatternProvider.EMPTY_PATTERN || this.pattern().isEmpty()

data class PatternKey(
    val pattern: String,
    val matchCase: Boolean = false
) : Key {
    fun getPatternFlag(): Int {
        return if (matchCase) 0 else Pattern.CASE_INSENSITIVE
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(pattern.toByteArray(Key.CHARSET))
    }
}