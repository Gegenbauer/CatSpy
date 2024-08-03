package me.gegenbauer.catspy.cache

import me.gegenbauer.catspy.cache.Reflections.ReflectionKey
import me.gegenbauer.catspy.context.ContextService
import java.lang.reflect.Field
import java.lang.reflect.Method

interface IReflection {

    fun setField(target: Any, fieldName: String, value: Any?)

    fun invokeMethod(target: Any, methodName: String, args: List<Pair<Any?, Class<*>>>): Any?
}

class Reflections : IReflection, LruCache<ReflectionKey, Any>(INITIAL_MAX_SIZE), ContextService {

    override fun create(key: ReflectionKey): Any? {
        return key.createAccessor()
    }

    override fun setField(target: Any, fieldName: String, value: Any?) {
        val key = FieldKey(target, fieldName)
        val accessor = get(key)
        if (accessor is Field) {
            try {
                accessor[target] = value
            } catch (e: IllegalAccessException) {
                CacheLog.d(TAG, "setField: ${e.message}")
            }
        }
    }

    override fun invokeMethod(target: Any, methodName: String, args: List<Pair<Any?, Class<*>>>): Any? {
        val key = MethodKey(target, methodName, args.map { it.second })
        val accessor = get(key)
        if (accessor is Method) {
            try {
                return accessor.invoke(target, *args.map { it.first }.toTypedArray())
            } catch (e: Exception) {
                CacheLog.d(TAG, "invokeMethod: ${e.message}")
            }
        }
        return null
    }

    interface ReflectionKey {
        val cachedKey: String

        fun createAccessor(): Any?
    }

    private class FieldKey(private val target: Any, private val fieldName: String) : ReflectionKey {
        override val cachedKey: String = "${KEY_PREFIX_FIELD}_${target.javaClass.name}_${fieldName}"

        override fun createAccessor(): Any? {
            return try {
                target::class.java.getDeclaredField(fieldName).apply { isAccessible = true }
            } catch (e: NoSuchFieldException) {
                CacheLog.d(TAG, "createAccessor: ${e.message}")
                null
            }
        }
    }

    private class MethodKey(
        private val target: Any,
        private val methodName: String,
        private val args: List<Class<*>>
    ) : ReflectionKey {
        override val cachedKey: String = run {
            "${KEY_PREFIX_METHOD}${target.javaClass.name}_${methodName}" + args.joinToString(
                prefix = "_",
                separator = "_"
            ) { it.javaClass.name }
        }

        override fun createAccessor(): Any? {
            return try {
                target::class.java.getDeclaredMethod(methodName, *args.toTypedArray()).also {
                    it.isAccessible = true
                }
            } catch (e: NoSuchMethodException) {
                CacheLog.d(TAG, "createAccessor: ${e.message}")
                null
            }
        }
    }

    companion object {
        private const val TAG = "Reflections"

        private const val KEY_PREFIX_FIELD = "field_"
        private const val KEY_PREFIX_METHOD = "method_"

        private const val INITIAL_MAX_SIZE = 1000L
    }
}