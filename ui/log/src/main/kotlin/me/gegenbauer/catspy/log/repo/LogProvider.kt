package me.gegenbauer.catspy.log.repo

import me.gegenbauer.catspy.log.model.LogItem

interface LogProvider<T: LogItem>: LogObservable.Observer<T>, LogObservable<T> {

    fun startCollectLog(collector: LogCollector<T>)

    fun stopCollectLog()

    fun clear()

    fun isCollecting(): Boolean

    fun destroy()
}