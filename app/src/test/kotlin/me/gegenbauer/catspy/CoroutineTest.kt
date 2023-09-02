package me.gegenbauer.catspy

import kotlinx.coroutines.*
import me.gegenbauer.catspy.concurrency.AppScope
import me.gegenbauer.catspy.concurrency.UI

fun main() {
    AppScope.launch(Dispatchers.UI) {
        async(CoroutineExceptionHandler { coroutineContext, throwable ->  }) { a() }
        delay(1000)
        cancel()
        println("end")
    }
}

suspend fun a() {
    repeat(Int.MAX_VALUE) {
        delay(200)
        println("1")
    }
}