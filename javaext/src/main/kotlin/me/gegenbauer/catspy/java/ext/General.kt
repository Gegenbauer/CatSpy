package me.gegenbauer.catspy.java.ext

inline fun <reified T: Enum<T>> getEnum(value: Int): T {
    for (enumValue in enumValues<T>()) {
        if (enumValue.ordinal == value) {
            return enumValue
        }
    }
    throw IllegalArgumentException("No enum constant ${T::class.simpleName} with value $value")
}