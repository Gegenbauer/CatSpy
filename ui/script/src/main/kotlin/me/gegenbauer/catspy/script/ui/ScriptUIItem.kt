package me.gegenbauer.catspy.script.ui

import me.gegenbauer.catspy.script.model.Script
import me.gegenbauer.catspy.script.parser.Rule

data class ScriptUIItem(
    val script: Script,
    val parseRule: Rule
)