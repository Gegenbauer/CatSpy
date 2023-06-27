package me.gegenbauer.catspy.script.parser

private const val OUTPUT_INVALID = "Invalid"

interface Rule {
    var nextRule: Rule?

    fun parse(input: String): List<String>
}

class ConditionalRule(
    private val condition: (String) -> Boolean,
    private val trueRule: Rule,
    private val falseRule: Rule? = null,
    override var nextRule: Rule? = null
) : Rule {
    override fun parse(input: String): List<String> =
        if (condition(input)) {
            trueRule.parse(input)
        } else {
            falseRule?.parse(input) ?: arrayListOf(OUTPUT_INVALID)
        }
}

class RegexRule(private val pattern: String, override var nextRule: Rule? = null) : Rule {
    override fun parse(input: String): List<String> {
        val regex = Regex(pattern)
        val matchResult = regex.find(input)
        // get all matched values
        val values = matchResult?.groupValues?.drop(1) ?: arrayListOf()
        return arrayListOf<String>().apply {
            values.forEach {
                addAll(nextRule?.parse(it) ?: arrayListOf(input))
            }
        }
    }
}

class DirectRule(override var nextRule: Rule? = null) : Rule {
    override fun parse(input: String): List<String> =
        nextRule?.parse(input) ?: arrayListOf(input)
}