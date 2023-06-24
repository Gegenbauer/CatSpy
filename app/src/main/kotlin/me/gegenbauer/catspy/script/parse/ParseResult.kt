package me.gegenbauer.catspy.script.parse

import me.gegenbauer.catspy.script.model.ValueHolder

interface ParseResult<T> {
    val value: ValueHolder<T>
}