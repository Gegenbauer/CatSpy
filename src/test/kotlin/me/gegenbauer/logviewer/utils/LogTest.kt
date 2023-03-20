package me.gegenbauer.logviewer.utils

import kotlinx.coroutines.*
import me.gegenbauer.logviewer.log.GLog

fun main() {
    runBlocking {
        val a = ClassA()
        val b = ClassB()
        val deferred1 = async {
            delay(1)
            repeat(1000) {
                delay(1)
                a.log()
            }
        }
        val deferred2 = async {
            repeat(1000) {
                delay(1)
                b.log()
            }
        }
        awaitAll(deferred1, deferred2)
    }
    GLog.d(TAG, "main")
    GLog.e(TAG, "main", Exception("test"))
}

private const val TAG = "LogTest"

class ClassA {

    fun log() {
        GLog.d(TAG, "logA")
    }
    companion object {
        private const val TAG = "ClassA"
    }
}

class ClassB {

    fun log() {
        GLog.d(TAG, "logB")
    }
    companion object {
        private const val TAG = "ClassB"
    }
}