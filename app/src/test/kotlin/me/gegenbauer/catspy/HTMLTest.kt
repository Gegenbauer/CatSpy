package me.gegenbauer.catspy

fun main() {
    val raw = "This is a sentence that needs some formatting."
    val renderer = HtmlStringRender()
    renderer.bold(0, 5)
//    renderer.highlight(0, 3)
    renderer.italic(4, 12)
    val result = renderer.render(raw)
    println(result)
}