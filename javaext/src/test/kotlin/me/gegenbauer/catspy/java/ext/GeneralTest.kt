package me.gegenbauer.catspy.java.ext

import kotlin.test.Test

class GeneralTest {

    @Test
    fun `should return object with null string properties replaced by default object when target has null property`() {
        val target = Custom("a", "b", "c", "d", null, "f", "g", "h", "i", "j")
        val default = Custom("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")
        replaceNullStringProperties(target, default)
        assert(target == default)
    }

    @Test
    fun `should return passed object when corresponding property of default object is null`() {
        val target = Custom("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")
        val source = target.copy()
        val default =  Custom("a", "b", "c", "d", null, "f", "g", "h", "i", "j")
        replaceNullStringProperties(target, default)
        assert(source == target)
    }

    @Test
    fun `should return passed object when default object is null`() {
        val target = Custom("a", "b", "c", "d", "e", "f", "g", "h", "i", "j")
        val source = target.copy()
        replaceNullStringProperties(target, null)
        assert(source == target)
    }

    @Test
    fun `should return object with null properties replaced by default object recursively when inner object has null property`() {
        val target = Custom("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", CustomInner("k", null))
        val default = Custom("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", CustomInner("k", "l"))
        replaceNullStringProperties(target, default)
        assert(target == default)
    }

    @Test
    fun `should return object with null properties replaced by default object recursively when inner and outer object has null property`() {
        val target = Custom("a", "b", "c", "d", "e", null, "g", "h", "i", "j", CustomInner("k", null))
        val default = Custom("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", CustomInner("k", "l"))
        replaceNullStringProperties(target, default)
        assert(target == default)
    }

    data class Custom(
        val a: String?,
        val b: String?,
        val c: String?,
        val d: String?,
        val e: String?,
        val f: String?,
        val g: String?,
        val h: String?,
        val i: String?,
        val j: String?,
        val obj: CustomInner? = null,
    )

    data class CustomInner(
        val k: String?,
        val l: String?,
    )
}