package me.gegenbauer.catspy.ddmlib.log

import org.slf4j.ILoggerFactory
import org.slf4j.Logger


class AdmLoggerFactory: ILoggerFactory {

    override fun getLogger(name: String): Logger {
        return AdmLogger(name)
    }
}