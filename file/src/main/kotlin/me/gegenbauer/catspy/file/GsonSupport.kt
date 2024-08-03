package me.gegenbauer.catspy.file

import com.google.gson.Gson
import com.google.gson.GsonBuilder

val gson: Gson = GsonBuilder().setPrettyPrinting().create()

fun <T: Any> clone(obj: T): T {
    return gson.fromJson(gson.toJson(obj), obj.javaClass) as T
}