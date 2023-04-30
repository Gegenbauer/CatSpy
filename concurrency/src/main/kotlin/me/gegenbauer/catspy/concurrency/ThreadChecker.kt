package me.gegenbauer.catspy.concurrency

import me.gegenbauer.catspy.log.GLog
import javax.swing.SwingUtilities

private const val TAG = "ThreadChecker"

fun assertInMainThread() {
    if (!isInMainThread()) {
        if (GLog.DEBUG) {
            throw AssertionError("This method must be executed on the main thread !!!")
        } else {
            GLog.w(TAG, "This method must be executed on the main thread !!!")
        }
    }
}

fun isInMainThread() = SwingUtilities.isEventDispatchThread()