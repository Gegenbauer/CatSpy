package me.gegenbauer.catspy.script.model

interface ValueParent<T>: ValueHolder<T> {

    val children: List<ValueHolder<T>>

    fun addChild(child: ValueHolder<T>)

    fun removeChild(child: ValueHolder<T>)

    fun removeChildAt(index: Int)

    fun clearChildren()

    fun getChildAt(index: Int): ValueHolder<T>
}