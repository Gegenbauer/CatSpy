package me.gegenbauer.catspy.cache

import me.gegenbauer.catspy.cache.Reflections.ReflectionKey
import me.gegenbauer.catspy.context.ContextService
import java.lang.reflect.Field
import java.lang.reflect.Method

interface IReflection {

    fun setField(target: Any, fieldName: String, value: Any?)

    fun getFieldValue(target: Any, fieldName: String): Any?

    fun getField(target: Any, fieldName: String): Field?

    fun invokeMethod(target: Any, methodName: String, args: List<Pair<Any?, Class<*>>>): Any?

    fun getMethod(target: Any, methodName: String, args: List<Class<*>>): Method?
}

class Reflections : IReflection, LruCache<ReflectionKey, Any>(INITIAL_MAX_SIZE), ContextService {

    override fun create(key: ReflectionKey): Any? {
        return key.createAccessor()
    }

    override fun setField(target: Any, fieldName: String, value: Any?) {
        val key = FieldKey(target, fieldName)
        val accessor = get(key)
        if (accessor is Field) {
            accessor[target] = value
        }
    }

    override fun getFieldValue(target: Any, fieldName: String): Any? {
        val key = FieldKey(target, fieldName)
        val accessor = get(key)
        if (accessor is Field) {
            return accessor[target]
        }
        return null
    }

    override fun getField(target: Any, fieldName: String): Field? {
        val key = FieldKey(target, fieldName)
        val accessor = get(key)
        if (accessor is Field) {
            return accessor
        }
        return null
    }

    override fun invokeMethod(target: Any, methodName: String, args: List<Pair<Any?, Class<*>>>): Any? {
        val key = MethodKey(target, methodName, args.map { it.second })
        val accessor = get(key)
        if (accessor is Method) {
            return accessor.invoke(target, *args.map { it.first }.toTypedArray())
        }
        return null
    }

    override fun getMethod(target: Any, methodName: String, args: List<Class<*>>): Method? {
        val key = MethodKey(target, methodName, args)
        val accessor = get(key)
        if (accessor is Method) {
            return accessor
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
            return target::class.java.getDeclaredField(fieldName).apply { isAccessible = true }
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
            return target::class.java.getDeclaredMethod(methodName, *args.toTypedArray()).also {
                it.isAccessible = true
            }
        }
    }

    companion object {
        private const val KEY_PREFIX_FIELD = "field_"
        private const val KEY_PREFIX_METHOD = "method_"

        private const val INITIAL_MAX_SIZE = 1000L
    }
}