package me.gegenbauer.catspy.ui.dialog

import kotlinx.coroutines.CoroutineScope
import me.gegenbauer.catspy.configuration.SettingsContainer
import me.gegenbauer.catspy.configuration.SettingsGroup

abstract class BaseSettingsGroup(
    title: String,
    protected val scope: CoroutineScope,
    protected val container: SettingsContainer
) : SettingsGroup(title)