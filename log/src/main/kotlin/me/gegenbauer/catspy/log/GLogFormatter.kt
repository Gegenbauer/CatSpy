package me.gegenbauer.catspy.log

import com.github.weisj.darklaf.util.StringUtil
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord

class GLogFormatter: Formatter() {
    private val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
        .withLocale(Locale.UK)
        .withZone(ZoneId.systemDefault())

    override fun format(record: LogRecord): String {
        val builder = StringBuilder()
        builder.append(ANSI_BLUE)
        val time = calculateDateString(record.millis)
        builder.append("[")
        builder.append(time)
        builder.append("]")
        builder.append(ANSI_YELLOW)
        builder.append(" [")
        builder.append(record.level.name)
        builder.append("]")
        builder.append(ANSI_RESET)
        builder.append(ANSI_BOLD_ON)
        builder.append(" [")
        builder.append(record.loggerName)
        builder.append("]")
        builder.append(ANSI_BOLD_OFF)
        builder.append(getMessageColor(record))
        builder.append(" ")
        builder.append(record.message)
        builder.append(ANSI_RESET)
        val params = record.parameters
        val spaceLength = time.length + 3 + record.level.name.length + 3
        val space = StringUtil.repeat(" ", spaceLength)
        if (params != null) {
            builder.append("\n")
            builder.append(StringUtil.repeat(" ", spaceLength - 10))
            builder.append(ANSI_YELLOW)
            builder.append("[Details] ")
            builder.append(getMessageColor(record))
            for (i in params.indices) {
                builder.append(params[i])
                if (i < params.size - 1) {
                    builder.append(",\n")
                    builder.append(space)
                }
            }
        }
        builder.append(ANSI_RESET)
        builder.append("\n")
        if (record.thrown != null) {
            builder.append(getMessageColor(record))
            appendExceptionMessage(builder, record.thrown)
        }
        return builder.toString()
    }

    private fun appendExceptionMessage(builder: StringBuilder, throwable: Throwable) {
        builder.append(throwable.javaClass.canonicalName).append(": ")
        builder.append(throwable.message)
        builder.append('\n')
        val trace = throwable.stackTrace
        for (element in trace) {
            builder.append("\tat ").append(element).append('\n')
        }
        val dejaVu = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        // Print suppressed exceptions, if any
        for (se in throwable.suppressed) {
            printEnclosedStackTrace(builder, se, trace, "Suppressed: ", "\t", dejaVu)
        }

        // Print cause, if any
        val cause = throwable.cause
        if (cause != null) {
            printEnclosedStackTrace(builder, cause, trace, "Caused by: ", "", dejaVu)
        }
    }

    private fun printEnclosedStackTrace(
        builder: StringBuilder,
        throwable: Throwable,
        enclosingTrace: Array<StackTraceElement>,
        caption: String,
        prefix: String,
        dejaVu: MutableSet<Throwable>
    ) {
        if (dejaVu.contains(throwable)) {
            builder.append(prefix).append(caption).append("[CIRCULAR REFERENCE: ").append(this).append("]\n")
        } else {
            dejaVu.add(throwable)
            // Compute number of frames in common between this and enclosing trace
            val trace = throwable.stackTrace
            var m = trace.size - 1
            var n = enclosingTrace.size - 1
            while (m >= 0 && n >= 0 && trace[m] == enclosingTrace[n]) {
                m--
                n--
            }
            val framesInCommon = trace.size - 1 - m

            // Print the stack trace
            builder.append(prefix).append(caption).append(throwable).append('\n')
            for (i in 0..m) {
                builder.append(prefix).append("\tat ").append(trace[i])
            }
            if (framesInCommon != 0) {
                builder.append(prefix).append("\t... ").append(framesInCommon).append(" more\n")
            }

            // Print suppressed exceptions, if any
            for (se in throwable.suppressed) {
                printEnclosedStackTrace(builder, se, trace, "Suppressed: ", prefix + "\t", dejaVu)
            }

            // Print cause, if any
            val cause = throwable.cause
            if (cause != null) {
                printEnclosedStackTrace(builder, cause, trace, "Caused by: ", prefix, dejaVu)
            }
        }
    }

    private fun calculateDateString(milliseconds: Long): String {
        return dateTimeFormatter.format(Instant.ofEpochMilli(milliseconds))
    }

    private fun getMessageColor(record: LogRecord): String {
        return if (record.level.intValue() >= Level.SEVERE.intValue()) {
            ANSI_RED
        } else if (record.level.intValue() >= Level.WARNING.intValue()) {
            ANSI_YELLOW
        } else {
            ANSI_BLACK
        }
    }

    companion object {
        private const val ANSI_RESET = "\u001B[0m"
        private const val ANSI_BLACK = "\u001B[30m"
        private const val ANSI_RED = "\u001B[31m"
        private const val ANSI_GREEN = "\u001B[32m"
        private const val ANSI_YELLOW = "\u001B[33m"
        private const val ANSI_BLUE = "\u001B[34m"
        private const val ANSI_PURPLE = "\u001B[35m"
        private const val ANSI_CYAN = "\u001B[36m"
        private const val ANSI_WHITE = "\u001B[37m"
        private const val ANSI_BOLD_ON = "\u001B[01m"
        private const val ANSI_BOLD_OFF = "\u001B[2m"
    }
}