package me.gegenbauer.catspy.log

import me.gegenbauer.catspy.configuration.GlobalStrings
import me.gegenbauer.catspy.databinding.bind.ObservableValueProperty
import me.gegenbauer.catspy.log.filter.StorableValueProperty

object LogConfSync {
    val showLogToolbar: ObservableValueProperty<Boolean> = StorableValueProperty(
        GlobalStrings.FUNC_SHOW_LOG_TOOLBAR,
        true
    )
    val showFilterPanel: ObservableValueProperty<Boolean> = StorableValueProperty(
        GlobalStrings.FUNC_SHOW_FILTER_PANEL,
        true
    )
    val showLogPanelToolbar: ObservableValueProperty<Boolean> = StorableValueProperty(
        GlobalStrings.FUNC_SHOW_LOG_PANEL_TOOLBAR,
        true
    )
    val showLogTableColumnNames: ObservableValueProperty<Boolean> = StorableValueProperty(
        GlobalStrings.FUNC_SHOW_LOG_TABLE_COLUMN_NAMES,
        true
    )
}