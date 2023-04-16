package me.gegenbauer.logviewer.databinding.property.support

interface DisposableAdapter {
    fun dispose() {
        this::class.java.methods
            .filter { it.isAnnotationPresent(Disposable::class.java) }
            .forEach { it.invoke(this) }
    }
}