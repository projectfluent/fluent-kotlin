package org.projectfluent.syntax.parser

import kotlin.Exception

class ParseError(var code: String, vararg args: Any) : Exception(getErrorMessage(code, *args)) {
    val args: MutableList<Any> = mutableListOf()

    init {
        this.args.addAll(args)
    }
}

fun getErrorMessage(code: String, vararg args: Any) : String {
    return when(code) {
        "E0001" -> "Generic Error"
        "E0002" -> "Expected an entry start"
        "E0003" -> "Expected token: \"${args[0]}\""
        "E0004" -> "Expected a character from range: \"${args[0]}\""
        "E0005" -> "Expected message \"${args[0]}\" to have a value or attributes"
        "E0006" -> "Expected term \"-${args[0]}\" to have a value"
        "E0007" -> "Keyword cannot end with a whitespace"
        "E0008" -> "The callee has to be an upper-case identifier or a term"
        "E0009" -> "The argument name has to be a simple identifier"
        "E0010" -> "Expected one of the variants to be marked as default (*)"
        "E0011" -> "Expected at least one variant after \"->\""
        "E0012" -> "Expected value"
        "E0013" -> "Expected variant key"
        "E0014" -> "Expected literal"
        "E0015" -> "Only one variant can be marked as default (*)"
        "E0016" -> "Message references cannot be used as selectors"
        "E0017" -> "Terms cannot be used as selectors"
        "E0018" -> "Attributes of messages cannot be used as selectors"
        "E0019" -> "Attributes of terms cannot be used as placeables"
        "E0020" -> "Unterminated string expression"
        "E0021" -> "Positional arguments must not follow named arguments"
        "E0022" -> "Named arguments must be unique"
        "E0024" -> "Cannot access variants of a message."
        "E0025" -> "Unknown escape sequence: \\${args[0]}."
        "E0026" -> "Invalid Unicode escape sequence: ${args[0]}."
        "E0027" -> "Unbalanced closing brace in TextElement."
        "E0028" -> "Expected an inline expression"
        else -> code
    }
}
