package me.gegenbauer.catspy.java.ext

inline fun <reified T : Enum<T>> getEnum(value: Int): T {
    for (enumValue in enumValues<T>()) {
        if (enumValue.ordinal == value) {
            return enumValue
        }
    }
    throw IllegalArgumentException("No enum constant ${T::class.simpleName} with value $value")
}

/**
 * replace null string properties, if target property is null, then replace it with default property
 * if target property is an object, then replace its properties recursively
 */
fun replaceNullStringProperties(target: Any, default: Any?) {
    default ?: return
    val targetClazz = target::class.java
    val defaultClazz = default::class.java
    require(targetClazz == defaultClazz) { "target and default must be the same class" }
    val targetFields = targetClazz.declaredFields
    val defaultFields = defaultClazz.declaredFields
    targetFields.forEach { field ->
        field.isAccessible = true
        if (field[target] == null) {
            field[target] = defaultFields.firstOrNull { it.name == field.name }
                ?.also { it.isAccessible = true }?.get(default)
            return@forEach
        }
        if (field.type != String::class.java) {
            replaceNullStringProperties(field[target],
                defaultFields.firstOrNull { it.name == field.name }
                    ?.also { it.isAccessible = true }?.get(default)
            )
        }
    }
}