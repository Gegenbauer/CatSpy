package me.gegenbauer.catspy

import kotlinx.coroutines.*
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.UI
import kotlin.random.Random

fun main() {
    runBlocking {
        val a = getRandom()
        val b = getRandom()
        calculateSum(a, b)
        println("end")
    }
}

suspend fun getRandom(): Int {
    delay(1000)
    return Random.Default.nextInt()
}

suspend fun calculateSum(a: Int, b: Int): Int {
    delay(1000)
    return a + b
}