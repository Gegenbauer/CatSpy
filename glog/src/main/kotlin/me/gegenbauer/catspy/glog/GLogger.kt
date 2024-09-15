package me.gegenbauer.catspy.glog

interface GLogger: LogFilterable {
    fun v(tag: String, msg: String)

    fun d(tag: String, msg: String)

    fun i(tag: String, msg: String)

    fun w(tag: String, msg: String)

    fun w(tag: String, msg: String, tr: Throwable?)

    fun e(tag: String, msg: String)

    fun e(tag: String, msg: String, tr: Throwable?)

    fun flush() {}
}

object EmptyLogger : GLogger {
    override fun v(tag: String, msg: String) {}

    override fun d(tag: String, msg: String) {}

    override fun i(tag: String, msg: String) {}

    override fun w(tag: String, msg: String) {}

    override fun w(tag: String, msg: String, tr: Throwable?) {}

    override fun e(tag: String, msg: String) {}

    override fun e(tag: String, msg: String, tr: Throwable?) {}

    override fun setFilter(filter: LogFilter) {}
}