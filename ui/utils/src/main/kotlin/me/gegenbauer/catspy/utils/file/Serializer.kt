package me.gegenbauer.catspy.utils.file

interface Serializer<T, K> {

    fun serialize(target: T): K

    fun deserialize(serialized: K): T
}