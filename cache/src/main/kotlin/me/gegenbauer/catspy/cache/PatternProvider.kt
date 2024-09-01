package me.gegenbauer.catspy.cache

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextService
import me.gegenbauer.catspy.context.MemoryAware
import me.gegenbauer.catspy.java.ext.EMPTY_STRING
import java.util.regex.Pattern

class PatternProvider(size: Long = DEFAULT_CACHE_SIZE) : ContextService, LruCache<PatternKey, Pattern>(size) {

    override fun create(key: PatternKey): Pattern? {
        return Pattern.compile(key.pattern, key.getPatternFlag())
    }

    override fun onContextDestroyed(context: Context) {
        clearMemory()
    }

    override fun onTrimMemory(level: MemoryAware.Level) {
        setSizeMultiplier(0.8f)
    }

    companion object {
        val EMPTY_PATTERN: Pattern = Pattern.compile(EMPTY_STRING)
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
}