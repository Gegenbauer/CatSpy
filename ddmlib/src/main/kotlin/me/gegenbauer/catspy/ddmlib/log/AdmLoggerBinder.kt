package me.gegenbauer.catspy.ddmlib.log

import org.slf4j.ILoggerFactory
import org.slf4j.IMarkerFactory
import org.slf4j.helpers.BasicMDCAdapter
import org.slf4j.helpers.BasicMarkerFactory
import org.slf4j.spi.MDCAdapter
import org.slf4j.spi.SLF4JServiceProvider


class AdmLoggerBinder: SLF4JServiceProvider {

    override fun getLoggerFactory(): ILoggerFactory {
        return AdmLoggerFactory()
    }

    override fun getMarkerFactory(): IMarkerFactory {
        return BasicMarkerFactory()
    }

    override fun getMDCAdapter(): MDCAdapter {
        return BasicMDCAdapter()
    }

    override fun getRequestedApiVersion(): String {
        return "2.0"
    }

    override fun initialize() {
        // do nothing
    }

}