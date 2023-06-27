package me.gegenbauer.catspy.script.parser

import me.gegenbauer.catspy.script.model.ValueHolder

interface ParseResult<T> {
    val value: ValueHolder<T>
}