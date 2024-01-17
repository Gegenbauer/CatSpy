package me.gegenbauer.catspy.file

import com.google.gson.GsonBuilder

val gson = GsonBuilder().setPrettyPrinting().create()

fun <T: Any> clone(obj: T): T {
    return gson.fromJson(gson.toJson(obj), obj.javaClass) as T
}