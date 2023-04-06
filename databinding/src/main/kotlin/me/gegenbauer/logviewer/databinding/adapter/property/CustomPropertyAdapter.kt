package me.gegenbauer.logviewer.databinding.adapter.property

import me.gegenbauer.logviewer.databinding.adapter.ComponentAdapter

interface CustomPropertyAdapter<T>: ComponentAdapter {

    fun updateValue(value: T?)
}