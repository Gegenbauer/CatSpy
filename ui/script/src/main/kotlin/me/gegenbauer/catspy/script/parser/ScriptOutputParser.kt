package me.gegenbauer.catspy.script.parser

class ScriptOutputParser(private val rule: Rule) {

    fun parse(output: String): List<String> {
        return rule.parse(output)
    }
}