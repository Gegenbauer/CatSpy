package me.gegenbauer.catspy.cache

import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.ContextService
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException
import kotlin.concurrent.write

class PatternProvider(override val size: Int = DEFAULT_CACHE_SIZE) : Cacheable<Pattern, PatternKey>, ContextService {
    // lru cache, remove the oldest entry when the cache is full
    // index 0 is the oldest entry
    private val patternCache = linkedMapOf<PatternKey, Pattern>()
    private val lock = ReentrantReadWriteLock()

    override fun clear() {
        lock.write { patternCache.clear() }
    }

    override fun clear(key: PatternKey) {
        lock.write { patternCache.remove(key) }
    }

    @Throws(PatternSyntaxException::class)
    override fun get(key: PatternKey): Pattern {
        if (key.pattern.isEmpty()) return emptyPattern
        return lock.write {
            if (patternCache.containsKey(key)) {
                val pattern = patternCache[key]!!
                patternCache[key] = pattern
                pattern
            } else {
                if (patternCache.size >= size) {
                    patternCache.remove(patternCache.keys.first())
                }
                Pattern.compile(key.pattern, key.getPatternFlag()).apply {
                    patternCache[key] = this
                }
            }
        }
    }

    override fun onContextDestroyed(context: Context) {
        clear()
    }

    companion object {
        val emptyPattern = Pattern.compile("")
        private const val DEFAULT_CACHE_SIZE = 1000

        fun String.toPatternKey(matchCase: Boolean = false) = PatternKey(this, matchCase)
    }

}

data class PatternKey(
    val pattern: String,
    val matchCase: Boolean = false
) {
    fun getPatternFlag(): Int {
        return if (matchCase) Pattern.CASE_INSENSITIVE else 0
    }
}