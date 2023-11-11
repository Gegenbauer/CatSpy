package me.gegenbauer.catspy.ui.panel

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.gegenbauer.catspy.context.Context
import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.file.GB
import me.gegenbauer.catspy.file.KB
import me.gegenbauer.catspy.glog.GLog
import me.gegenbauer.catspy.ui.MainFrame
import me.gegenbauer.catspy.ui.MainViewModel
import me.gegenbauer.catspy.ui.Memory
import me.gegenbauer.catspy.ui.MemoryMonitor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JProgressBar

class MemoryStatusBar(override val contexts: Contexts = Contexts.default) : JProgressBar(), Context {
    private val scope = MainScope()
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
                Runtime.getRuntime().gc()
                getMainViewModel()?.refreshMemoryInfo()
            }
        })
    }

    override fun configureContext(context: Context) {
        super.configureContext(context)
        getMainViewModel()?.let { vm ->
            scope.launch {
                vm.memoryInfo.collect {
                    setMemoryInfo(it)
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
        maximum = (memory.max / KB).toInt()
        string = memory.readable()
        value = (memory.allocated / KB).toInt()
        setMemoryState(memory)
    }

    private fun setMemoryState(memory: Memory) {
        val property = if (memory.allocated < memoryLimit) {
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