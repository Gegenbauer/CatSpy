package me.gegenbauer.catspy.script.model

class TreeStringValue(override val value: String) : ValueParent<String> {

    override val children: MutableList<ValueHolder<String>> = arrayListOf()

    override fun removeChildAt(index: Int) {
        children.removeAt(index)
    }

    override fun clearChildren() {
        children.clear()
    }

    override fun getChildAt(index: Int): ValueHolder<String> {
        return children[index]
    }

    override fun removeChild(child: ValueHolder<String>) {
        children.remove(child)
    }

    override fun addChild(child: ValueHolder<String>) {
        children.add(child)
    }
}