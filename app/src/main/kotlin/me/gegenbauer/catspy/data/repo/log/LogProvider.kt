package me.gegenbauer.catspy.data.repo.log

import me.gegenbauer.catspy.data.model.log.LogItem

interface LogProvider<T: LogItem>: LogObservable.Observer<T>, LogObservable<T> {

    fun startCollectLog(collector: LogCollector<T>)

    fun stopCollectLog()

    fun clear()

    fun isCollecting(): Boolean

    fun destroy()
}