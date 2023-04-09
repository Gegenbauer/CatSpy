package me.gegenbauer.logviewer.databinding

import me.gegenbauer.logviewer.log.GLog
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.isAccessible

private const val TAG = "Reflection"

fun Any.invokeMethod(methodName: String, vararg args: Any?) {
    kotlin.runCatching {
        val method = this::class.declaredMemberFunctions.firstOrNull { it.name == methodName }
        if (method != null && method.isAccessible) {
            method.call(this, *args)
        } else {
            GLog.w(TAG, "[invokeMethod] no such method: $methodName")
        }
    }.onFailure {
        GLog.e(TAG, "[invokeMethod] failed! methodName: $methodName, args: $args", it)
    }
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

fun String.capitalize() = this.replaceFirstChar { it.uppercase() }