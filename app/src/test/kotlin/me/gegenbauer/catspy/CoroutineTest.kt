package me.gegenbauer.catspy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

fun main() {
    runBlocking {
        launch(Dispatchers.Default) {
            coroutineContext.job.invokeOnCompletion {
                println("sub coroutine completed")
            }
            delay(100000)
        }
        coroutineContext.job.invokeOnCompletion {
            println("main coroutine completed")
        }
        cancel()
    }
}