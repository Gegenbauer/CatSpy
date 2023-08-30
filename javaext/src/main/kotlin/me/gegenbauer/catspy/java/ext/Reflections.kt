package me.gegenbauer.catspy.java.ext

import me.gegenbauer.catspy.glog.GLog
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

private const val TAG = "Reflection"

fun Any.invokeMethod(methodName: String, vararg args: Any?): Any? {
    return kotlin.runCatching {
        val method = this::class.declaredMemberFunctions.firstOrNull { it.name == methodName }
        method?.isAccessible = true
        if (method != null && method.isAccessible) {
            method.call(this, *args)
        } else {
            GLog.w(TAG, "[invokeMethod] no such method: $methodName")
            null
        }
    }.onFailure {
        GLog.e(TAG, "[invokeMethod] failed! methodName: $methodName, args: $args", it)
    }.getOrNull()
}

fun Any.setField(fieldName: String, value: Any?) {
    kotlin.runCatching {
        val property = this::class.declaredMemberProperties.firstOrNull { it.name == fieldName }
        if (property != null && property is KMutableProperty1) {
            property.isAccessible = true
            property.setter.call(this, value)
        } else {
            invokeMethod("set${fieldName.capitalize()}", value)
        }
    }.onFailure {
        GLog.e(TAG, "[setField] failed! $this fieldName: $fieldName, value: $value", it)
    }
}

/**
 * 从本类查询 field, 如果没有匹配到，则从父类查询，依次类推
 */
fun Any.getFieldDeeply(fieldName: String): Field {
    var clazz: Class<*>? = this::class.java
    while (clazz != null) {
        val clazzNonnull = clazz
        kotlin.runCatching {
            val field = clazzNonnull.getDeclaredField(fieldName)
            field.isAccessible = true
            return field
        }.onFailure {
            clazz = clazzNonnull.superclass
        }
    }
    throw NoSuchFieldException("no such field: $fieldName")
}

fun Any.getMethodDeeply(methodName: String, vararg args: Class<*>?): Method? {
    var clazz: Class<*>? = this::class.java
    while (clazz != null) {
        val clazzNonnull = clazz
        kotlin.runCatching {
            val method = clazzNonnull.getDeclaredMethod(methodName, *args)
            method.isAccessible = true
            return method
        }.onFailure {
            clazz = clazzNonnull.superclass
        }
    }
    throw NoSuchMethodException("no such method: $methodName")
}