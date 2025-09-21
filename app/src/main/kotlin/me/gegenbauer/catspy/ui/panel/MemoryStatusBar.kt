package me.gegenbauer.catspy.ui.panel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.concurrency.CPU
import me.gegenbauer.catspy.concurrency.UIScope
import me.gegenbauer.catspy.context.*
import me.gegenbauer.catspy.file.GB
import me.gegenbauer.catspy.file.KB
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.platform.isInDebugMode
import me.gegenbauer.catspy.ui.MainFrame
import me.gegenbauer.catspy.ui.MainViewModel
import me.gegenbauer.catspy.ui.MemoryMonitor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JProgressBar

class MemoryStatusBar(override val contexts: Contexts = Contexts.default) : JProgressBar(), Context {
    private val scope = UIScope()
    private val memoryLimit by lazy {
        (Runtime.getRuntime().maxMemory() - MemoryMonitor.minFreeMemory).also {
            GLog.d(TAG, "[memoryLimit] ${it / GB} GB")
        }
    }

    init {
        isStringPainted = true
        putClientProperty(PROGRESS_BAR_STATUS_PROPERTY_PASS, true)

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                scope.launch(Dispatchers.CPU) {
                    MemoryState.forceTrimMemory()
                    Runtime.getRuntime().gc()
                    getMainViewModel()?.refreshMemoryInfo()
                }
            }
        })
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        getMainViewModel()?.let { vm ->
            scope.launch {
                vm.eventFlow.collect {
                    if (it is Memory) {
                        setMemoryInfo(it)
                    }
                }
            }
        }
    }

    private fun getMainViewModel(): MainViewModel? {
        return contexts.getContext(MainFrame::class.java)?.let { frame ->
            ServiceManager.getContextService(frame, MainViewModel::class.java)
        }
    }

    private fun setMemoryInfo(memory: Memory) {
        maximum = (memory.jvm.max / KB).toInt()
        string = memory.jvm.readable()
        value = (memory.jvm.allocated / KB).toInt()
        if (memory.device.isNotEmpty() && isInDebugMode) {
            toolTipText = "Used: ${memory.device.readable()}"
        }
        setMemoryState(memory)
    }

    private fun setMemoryState(memory: Memory) {
        val property = if (memory.jvm.allocated < memoryLimit) {
            PROGRESS_BAR_STATUS_PROPERTY_PASS
        } else {
            PROGRESS_BAR_STATUS_PROPERTY_FAIL
        }
        putClientProperty(property, true)
    }

    override fun destroy() {
        super.destroy()
        scope.cancel()
    }

    companion object {
        private const val TAG = "MemoryStatusBar"
        private const val PROGRESS_BAR_STATUS_PROPERTY_PASS = "JProgressBar.passed"
        private const val PROGRESS_BAR_STATUS_PROPERTY_FAIL = "JProgressBar.failed"
    }

}