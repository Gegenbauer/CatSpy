package me.gegenbauer.catspy.java.ext

import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun formatDuration(milliseconds: Long): String {
    val duration = milliseconds.toDuration(DurationUnit.MILLISECONDS)

    val hours = duration.toDouble(DurationUnit.HOURS).toInt()
    val minutes = duration.toDouble(DurationUnit.MINUTES).toInt() % 60
    val seconds = duration.toDouble(DurationUnit.SECONDS).toInt() % 60
    val millis = duration.toDouble(DurationUnit.MILLISECONDS).toInt() % 1000

    return when {
        hours > 0 -> {
            String.format("%d h %d m %d s", hours, minutes, seconds)
        }
        minutes > 0 -> {
            String.format("%d m %d s", minutes, seconds)
        }
        seconds > 0 -> {
            String.format("%d s %d ms", seconds, millis)
        }
        else -> {
            String.format("%d ms", millis)
        }
    }
}