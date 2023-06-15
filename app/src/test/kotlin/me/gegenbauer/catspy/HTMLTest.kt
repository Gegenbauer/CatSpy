package me.gegenbauer.catspy

fun main() {
    val raw = "This is a sentence that needs some formatting."
    val render = HtmlStringRender()
    render.bold(0, 5)
//    render.highlight(0, 3)
    render.italic(4, 12)
    val result = render.render(raw)
    println(result)
}