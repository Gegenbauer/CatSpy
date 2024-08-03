package me.gegenbauer.catspy.log

import kotlinx.coroutines.*
import me.gegenbauer.catspy.log.serialize.LogMetadataSerializer
import me.gegenbauer.catspy.log.metadata.StandardDeviceLogMetadataProvider
import me.gegenbauer.catspy.log.metadata.StandardLogcatFileLogMetadataProvider
import kotlin.test.Test

class LogMetadataSerializerTest {

    @Test
    fun printStandardLogcatFileLogMetadata() {
        val logMetadata = runBlocking {
            StandardLogcatFileLogMetadataProvider().getMetadata()
        }
        val serializer = LogMetadataSerializer()
        println(serializer.serialize(logMetadata))
    }

    @Test
    fun printStandardLogcatDeviceLogMetadata() {
        val logMetadata = runBlocking {
            StandardDeviceLogMetadataProvider().getMetadata()
        }
        val serializer = LogMetadataSerializer()
        println(serializer.serialize(logMetadata))
    }

    @Test
    fun testScope() {
        val job = GlobalScope.launch {
            coroutineContext.job.invokeOnCompletion {
                println("outer job completed")
            }
            launch {
                coroutineContext.job.invokeOnCompletion {
                    println("inner job completed")
                }
                while (isActive) {}
            }
            while (isActive) {}
        }
        runBlocking {
            delay(19)
            job.cancelAndJoin()
            println()
        }
    }
}