package org.projectfluent.syntax.processor

import org.projectfluent.syntax.ast.* // ktlint-disable no-wildcard-imports
import java.lang.Exception

/**
 * Process patterns by returning new patterns with elements transformed.
 */
class Processor {
    /**
     * "Bake" the values of StringLiterals into TextElements. This is a lossy
     * transformation for literals which are not special in Fluent syntax.
     */
    fun unescapeLiteralsToText(pattern: Pattern): Pattern {
        val result = Pattern()
        for (elem in textFromLiterals(pattern)) {
            result.elements.add(elem)
        }
        return result
    }

    /**
     * "Un-bake" special characters into StringLiterals, which would otherwise
     * cause syntax errors with Fluent parsers.
     */
    fun escapeTextToLiterals(pattern: Pattern): Pattern {
        val result = Pattern()
        for (elem in literalsFromText(pattern)) {
            result.elements.add(elem)
        }
        return result
    }

    private fun textFromLiterals(pattern: Pattern) = sequence {
        var lastText: TextElement? = null
        pattern.elements.forEach { element ->
            when (element) {
                is TextElement -> {
                    if (lastText == null) {
                        lastText = element
                    } else {
                        lastText?.let { it.value += element.value }
                    }
                }
                is Placeable -> {
                    when (val expression = element.expression) {
                        is NumberLiteral -> {
                            val content = expression.value
                            if (lastText == null) {
                                lastText = TextElement("")
                            }
                            lastText?.let { it.value += content }
                        }
                        is StringLiteral -> {
                            var content = expression.value
                            content = special.replace(content) { m -> unescape(m) }
                            if (lastText == null) {
                                lastText = TextElement("")
                            }
                            lastText?.let { it.value += content }
                        }
                        is SelectExpression -> {
                            val processedVariants: MutableList<Variant> = mutableListOf()
                            for (variant in expression.variants) {
                                val processedVariant = Variant(variant.key, unescapeLiteralsToText(variant.value), variant.default)
                                processedVariants.add(processedVariant)
                            }
                            val processedSelect = SelectExpression(expression.selector, processedVariants)
                            val placeable = Placeable(processedSelect)

                            lastText?.let {
                                yield(it)
                                lastText = null
                            }
                            yield(placeable)
                        }
                        else -> {
                            lastText?.let {
                                yield(it)
                                lastText = null
                            }
                            yield(element)
                        }
                    }
                }
            }
        }
        lastText?.let { yield(it) }
    }

    private fun literalsFromText(pattern: Pattern) = sequence {
        pattern.elements.forEach { element ->
            when (element) {
                is TextElement -> {
                    if (element.value.startsWith(' ') || element.value.startsWith('\n')) {
                        val expr = StringLiteral("")
                        yield(Placeable(expr))
                    }

                    var startIndex = 0
                    for (i in element.value.indices) {
                        when (val char = element.value[i]) {
                            '{', '}' -> {
                                val before = element.value.substring(startIndex, i)
                                if (before.isNotEmpty()) {
                                    yield(TextElement(before))
                                }
                                val expr = StringLiteral(char.toString())
                                yield(Placeable(expr))
                                startIndex = i + 1
                            }
                            '[', '*', '.' -> {
                                if (i > 0 && element.value[i - 1] == '\n') {
                                    val before = element.value.substring(startIndex, i)
                                    yield(TextElement(before))
                                    val expr = StringLiteral(char.toString())
                                    yield(Placeable(expr))
                                    startIndex = i + 1
                                }
                            }
                        }
                    }

                    // Yield the remaining text.
                    if (element.value.lastIndex > startIndex) {
                        val text = element.value.substring(startIndex)
                        yield(TextElement(text))
                    }

                    if (element.value.endsWith(' ') || element.value.endsWith('\n')) {
                        val expr = StringLiteral("")
                        yield(Placeable(expr))
                    }
                }
                is Placeable -> {
                    when (val expression = element.expression) {
                        is SelectExpression -> {
                            val rawVariants: MutableList<Variant> = mutableListOf()
                            for (variant in expression.variants) {
                                val rawVariant = Variant(variant.key, escapeTextToLiterals(variant.value), variant.default)
                                rawVariants.add(rawVariant)
                            }
                            val rawSelect = SelectExpression(expression.selector, rawVariants)
                            val placeable = Placeable(rawSelect)
                            yield(placeable)
                        }
                        else -> {
                            yield(element)
                        }
                    }
                }
            }
        }
    }

    private val special =
        """\\(([\\"])|(u[0-9a-fA-F]{4}))""".toRegex()

    private fun unescape(matchResult: MatchResult): CharSequence {
        val matches = matchResult.groupValues.drop(2).listIterator()
        val simple = matches.next()
        if (simple != "") { return simple }
        val uni4 = matches.next()
        if (uni4 != "") {
            return uni4.substring(1).toInt(16).toChar().toString()
        }
        throw Exception("Unexpected")
    }
}
