package me.gegenbauer.catspy.log.datasource

import me.gegenbauer.catspy.context.Contexts
import me.gegenbauer.catspy.context.ServiceManager
import me.gegenbauer.catspy.databinding.bind.Observer
import me.gegenbauer.catspy.log.BookmarkManager
import me.gegenbauer.catspy.log.ui.tab.BaseLogMainPanel
import me.gegenbauer.catspy.log.ui.table.LogPanel

internal interface FilterState {
    val enabled: Boolean

    fun observe(observer: (Boolean) -> Unit)

    fun stopObserving()
}

internal abstract class BaseFilterState(private val contexts: Contexts) : FilterState, Observer<Boolean> {

    override val enabled: Boolean
        get() = _enabled

    private var _enabled: Boolean = false
    private var observer: ((Boolean) -> Unit)? = null

    protected fun getBindings(): LogPanel.LogPanelBinding? {
        return contexts.getContext(LogPanel::class.java)?.binding
    }

    override fun observe(observer: (Boolean) -> Unit) {
        this.observer = observer
    }

    override fun stopObserving() {
        observer = null
    }

    override fun onChange(newValue: Boolean?) {
        val enabled = newValue ?: false
        _enabled = enabled
        observer?.invoke(enabled)
    }
}

internal class BookmarkFilterState(private val contexts: Contexts) : BaseFilterState(contexts) {

    private val bookmarkManager: BookmarkManager by lazy {
        val logTabPanel = contexts.getContext(BaseLogMainPanel::class.java)!!
        ServiceManager.getContextService(logTabPanel, BookmarkManager::class.java)
    }

    override fun observe(observer: (Boolean) -> Unit) {
        super.observe(observer)
        getBindings()?.bookmarkMode?.addObserver(this)
    }

    override fun stopObserving() {
        super.stopObserving()
        getBindings()?.bookmarkMode?.removeObserver(this)
    }

    fun isBookmark(num: Int): Boolean {
        return bookmarkManager.isBookmark(num)
    }
}

internal class FullModeFilterState(contexts: Contexts) : BaseFilterState(contexts) {

    override fun observe(observer: (Boolean) -> Unit) {
        super.observe(observer)
        getBindings()?.fullMode?.addObserver(this)
    }

    override fun stopObserving() {
        super.stopObserving()
        getBindings()?.fullMode?.removeObserver(this)
    }
}