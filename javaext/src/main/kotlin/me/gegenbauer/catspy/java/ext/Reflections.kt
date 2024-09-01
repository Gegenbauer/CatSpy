package me.gegenbauer.catspy.java.ext

import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.kotlinProperty

class SetFieldException(message: String) : RuntimeException(message)

fun Any.setField(fieldName: String, value: Any?) {
    kotlin.runCatching {
        val method = this::class.java.methods.firstOrNull { it.name == "set${fieldName.capitalize()}" }
        method?.isAccessible = true
        method?.invoke(this, value)
    }.onFailure {
        throw SetFieldException("[setField] failed! $this fieldName: $fieldName, value: $value")
    }
}

/**
 * Query the field from the current class. If no match is found, query from the parent class, and so on.
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

fun copyFields(from: Any, to: Any) {
    require(from::class.java == to::class.java) { "target and default must be the same class" }
    val fromFields = from::class.java.declaredFields
    val toFields = to::class.java.declaredFields
    fromFields.forEach { field ->
        field.isAccessible = true
        // 不是 companion
        val toField = toFields.firstOrNull {
            it.name == field.name && !isCompanionObject(it.type, field.declaringClass)
                    && !isConstantField(it)
        }
        if (toField != null) {
            toField.isAccessible = true
            toField[to] = field[from]
        }
    }
}

fun isCompanionObject(javaClass: Class<*>, declaredClass: Class<*>): Boolean {
    val kClass = declaredClass.kotlin
    val companionObjectInstance = kClass.companionObjectInstance
    return javaClass.name == companionObjectInstance?.javaClass?.name
}

fun isConstantField(field: Field): Boolean {
    return field.kotlinProperty?.isConst == null
}

class KotlinReflectionPreTrigger {

    fun trigger() {
        val clazz = this::class
        clazz.declaredMemberProperties
        clazz.declaredMemberFunctions
    }
}