package me.gegenbauer.catspy.script.parse

class ScriptOutputParser(
    private val rule: Rule
) {

    fun parse(output: String): List<String> {
        return rule.parse(output)
    }
}