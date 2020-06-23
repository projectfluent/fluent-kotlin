package org.projectfluent.syntax.parser

import org.projectfluent.syntax.ast.* // ktlint-disable no-wildcard-imports

private val trailingWSRe = Regex("[ \t\n\r]+\$")
private val VALID_FUNCTION_NAME = Regex("^[A-Z][A-Z0-9_-]*\$")

/**
 * Parse Fluent resources.
 *
 * @property withSpans create source positions for nodes (not supported yet)
 */
class FluentParser(var withSpans: Boolean = false) {

    fun parse(source: String): Resource {
        val ps = FluentStream(source)
        val entries: MutableList<TopLevel> = mutableListOf()
        var lastComment: Comment? = null
        var blankLines = ps.skipBlankBlock()
        if (blankLines.isNotEmpty()) {
            entries.add(Whitespace(blankLines))
        }
        while (ps.currentChar() != null) {
            val entry = this.getEntryOrJunk(ps)
            blankLines = ps.skipBlankBlock()

            // Regular Comments require special logic. Comments may be attached to
            // Messages or Terms if they are followed immediately by them. However
            // they should parse as standalone when they're followed by Junk.
            // Consequently, we only attach Comments once we know that the Message
            // or the Term parsed successfully.
            if (entry is Comment &&
                    blankLines.isEmpty() &&
                    ps.currentChar() != EOF
            ) {
                // Stash the comment and decide what to do with it in the next pass.
                lastComment = entry
                continue
            }

            lastComment?.let {
                when (entry) {
                    is Message -> entry.comment = it
                    is Term -> entry.comment = it
                    else -> entries.add(it)
                }
                // In either case, the stashed comment has been dealt with; clear it.
                lastComment = null
            }

            // No special logic for other types of entries.
            entries.add(entry)
            if (blankLines.isNotEmpty()) {
                entries.add(Whitespace(blankLines))
            }
        }
        return Resource(*entries.toTypedArray())
    }

    private fun getEntryOrJunk(ps: FluentStream): TopLevel {
        val entryStartPos = ps.index

        try {
            val entry = this.getEntry(ps)
            ps.expectLineEnd()
            return entry
        } catch (err: ParseError) {

            var errorIndex = ps.index
            ps.skipToNextEntryStart(entryStartPos)
            val nextEntryStart = ps.index
            if (nextEntryStart < errorIndex) {
                // The position of the error must be inside of the Junk's span.
                errorIndex = nextEntryStart
            }

            // Create a Junk instance
            val slice = ps.string.substring(entryStartPos, nextEntryStart)
            val annot = Annotation(err.code, err.message ?: "", err.args, Span(errorIndex))
            return Junk(slice, listOf(annot))
        }
    }

    private fun getEntry(ps: FluentStream): Entry {
        if (ps.currentChar() == '#') {
            return this.getComment(ps)
        }

        if (ps.currentChar() == '-') {
            return this.getTerm(ps)
        }

        if (ps.isIdentifierStart()) {
            return this.getMessage(ps)
        }

        throw ParseError("E0002")
    }

    private fun getComment(ps: FluentStream): BaseComment {
        // 0 - comment
        // 1 - group comment
        // 2 - resource comment
        var level = -1
        var content = ""

        while (true) {
            var i = -1
            val thisLevel = if (level == -1) 2 else level
            while (ps.currentChar() == '#' && i < thisLevel) {
                ps.next()
                i++
            }

            if (level == -1) {
                level = i
            }

            if (ps.currentChar() != EOL) {
                ps.expectChar(' ')
                var ch = ps.takeChar { x -> x != EOL }
                while (ch != EOF) {
                    content += ch
                    ch = ps.takeChar { x -> x != EOL }
                }
            }

            if (ps.isNextLineComment(level)) {
                content += ps.currentChar()
                ps.next()
            } else {
                break
            }
        }

        return when (level) {
            0 -> Comment(content)
            1 -> GroupComment(content)
            else -> ResourceComment(content)
        }
    }

    private fun getMessage(ps: FluentStream): Message {
        val id = this.getIdentifier(ps)

        ps.skipBlankInline()
        ps.expectChar('=')

        val value = this.maybeGetPattern(ps)
        val attrs = this.getAttributes(ps)

        if (value == null && attrs.isEmpty()) {
            throw ParseError("E0005", id.name)
        }

        return Message(id, value, attrs)
    }

    private fun getTerm(ps: FluentStream): Term {
        ps.expectChar('-')
        val id = this.getIdentifier(ps)

        ps.skipBlankInline()
        ps.expectChar('=')

        val value = this.maybeGetPattern(ps)
        if (value === null) {
            throw ParseError("E0006", id.name)
        }

        val attrs = this.getAttributes(ps)
        return Term(id, value, attrs)
    }

    private fun getAttribute(ps: FluentStream): Attribute {
        ps.expectChar('.')

        val key = this.getIdentifier(ps)

        ps.skipBlankInline()
        ps.expectChar('=')

        val value = this.maybeGetPattern(ps) ?: throw ParseError("E0012")

        return Attribute(key, value)
    }

    private fun getAttributes(ps: FluentStream): List<Attribute> {
        val attrs: MutableList<Attribute> = mutableListOf()
        ps.peekBlank()
        while (ps.isAttributeStart()) {
            ps.skipToPeek()
            val attr = this.getAttribute(ps)
            attrs.add(attr)
            ps.peekBlank()
        }
        return attrs
    }

    private fun getIdentifier(ps: FluentStream): Identifier {
        var name = "" + ps.takeIDStart()
        var ch = ps.takeIDChar()
        while (ch != null) {
            name += ch
            ch = ps.takeIDChar()
        }
        return Identifier(name)
    }

    private fun getVariantKey(ps: FluentStream): VariantKey {
        val ch = ps.currentChar() ?: throw ParseError("E0013")

        val cc = ch.toInt()

        if ((cc in 48..57) || cc == 45) { // 0-9, -
            return this.getNumber(ps)
        }

        return this.getIdentifier(ps)
    }

    private fun getVariant(ps: FluentStream, hasDefault: Boolean = false): Variant {
        var defaultIndex = false

        if (ps.currentChar() == '*') {
            if (hasDefault) {
                throw ParseError("E0015")
            }
            ps.next()
            defaultIndex = true
        }

        ps.expectChar('[')

        ps.skipBlank()

        val key = this.getVariantKey(ps)

        ps.skipBlank()
        ps.expectChar(']')

        val value = this.maybeGetPattern(ps) ?: throw ParseError("E0012")

        return Variant(key, value, defaultIndex)
    }

    private fun getVariants(ps: FluentStream): MutableList<Variant> {
        val variants: MutableList<Variant> = mutableListOf()
        var hasDefault = false

        ps.skipBlank()
        while (ps.isVariantStart()) {
            val variant = this.getVariant(ps, hasDefault)

            if (variant.default) {
                hasDefault = true
            }

            variants.add(variant)
            ps.expectLineEnd()
            ps.skipBlank()
        }

        if (variants.size == 0) {
            throw ParseError("E0011")
        }

        if (!hasDefault) {
            throw ParseError("E0010")
        }

        return variants
    }

    private fun getDigits(ps: FluentStream): String {
        var num = ""

        var ch = ps.takeDigit()
        while (ch != null) {
            num += ch
            ch = ps.takeDigit()
        }

        if (num.isEmpty()) {
            throw ParseError("E0004", "0-9")
        }

        return num
    }

    private fun getNumber(ps: FluentStream): NumberLiteral {
        var value = ""

        value += if (ps.currentChar() == '-') {
            ps.next()
            "-${this.getDigits(ps)}"
        } else {
            this.getDigits(ps)
        }

        if (ps.currentChar() == '.') {
            ps.next()
            value += ".${this.getDigits(ps)}"
        }

        return NumberLiteral(value)
    }

    // maybeGetPattern distinguishes between patterns which start on the same line
    // as the identifier (a.k.a. inline signleline patterns and inline multiline
    // patterns) and patterns which start on a new line (a.k.a. block multiline
    // patterns). The distinction is important for the dedentation logic: the
    // indent of the first line of a block pattern must be taken into account when
    // calculating the maximum common indent.
    private fun maybeGetPattern(ps: FluentStream): Pattern? {
        ps.peekBlankInline()
        if (ps.isValueStart()) {
            ps.skipToPeek()
            return this.getPattern(ps, false)
        }

        ps.peekBlankBlock()
        if (ps.isValueContinuation()) {
            ps.skipToPeek()
            return this.getPattern(ps, true)
        }

        return null
    }

    private fun getPattern(ps: FluentStream, isBlock: Boolean): Pattern {
        val elements: MutableList<PatternElement> = mutableListOf()
        var commonIndentLength = if (isBlock) {
            // A block pattern is a pattern which starts on a new line. Store and
            // measure the indent of this first line for the dedentation logic.
            val blankStart = ps.index
            val firstIndent = ps.skipBlankInline()
            elements.add(this.getIndent(ps, firstIndent, blankStart))
            firstIndent.length
        } else {
            Int.MAX_VALUE
        }

        var ch: Char?
        elements@ while (true) {
            ch = ps.currentChar()
            if (ch == null) break
            when (ch) {
                EOL -> {
                    val blankStart = ps.index
                    val blankLines = ps.peekBlankBlock()
                    if (ps.isValueContinuation()) {
                        ps.skipToPeek()
                        val indent = ps.skipBlankInline()
                        commonIndentLength = minOf(commonIndentLength, indent.length)
                        elements.add(this.getIndent(ps, blankLines + indent, blankStart))
                        continue@elements
                    }

                    // The end condition for getPattern's while loop is a newline
                    // which is not followed by a valid pattern continuation.
                    ps.resetPeek()
                    break@elements
                }
                '{' -> {
                    elements.add(this.getPlaceable(ps))
                    continue@elements
                }
                '}' -> throw ParseError("E0027")
                else -> elements.add(this.getTextElement(ps))
            }
        }

        val dedented = this.dedent(elements, commonIndentLength)
        return Pattern(*dedented.toTypedArray())
    }

    // Create a token representing an indent. It's not part of the AST and it will
    // be trimmed and merged into adjacent TextElements, or turned into a new
    // TextElement, if it's surrounded by two Placeables.
    private fun getIndent(ps: FluentStream, value: String, start: Int): Indent {
        return Indent(value, Span(start, ps.index))
    }

    // Dedent a list of elements by removing the maximum common indent from the
    // beginning of text lines. The common indent is calculated in getPattern.
    private fun dedent(elements: Collection<PatternElement>, commonIndent: Int): List<PatternElement> {
        val trimmed: MutableList<PatternElement> = mutableListOf()

        for (element in elements) {
            if (element is Placeable) {
                trimmed.add(element)
                continue
            }

            if (element is Indent) {
                // Strip common indent.
                element.value = element.value.slice(
                        0 until (element.value.length - commonIndent)
                )
                if (element.value.isEmpty()) {
                    continue
                }
            }

            if (trimmed.isNotEmpty()) {
                val prev = trimmed.last()
                if (prev is TextElement) {
                    // Join adjacent TextElements by replacing them with their sum.
                    val span = if (this.withSpans) {
                        val start = prev.span?.start
                        val end = element.span?.end
                        if (start != null && end != null) {
                            Span(start, end)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                    val sum = TextElement(
                            prev.value + when (element) {
                                is TextElement -> element.value
                                is Indent -> element.value
                                else -> throw IllegalStateException("Unexpected PatternElement type")
                            },
                            span
                    )
                    trimmed[trimmed.size - 1] = sum
                    continue
                }
            }

            if (element is Indent) {
                // If the indent hasn't been merged into a preceding TextElement,
                // convert it into a new TextElement.
                val span = if (this.withSpans) {
                    val start = element.span?.start
                    val end = element.span?.end
                    if (start != null && end != null) {
                        Span(start, end)
                    } else {
                        null
                    }
                } else {
                    null
                }
                val textElement = TextElement(element.value, span)
                trimmed.add(textElement)
                continue
            }

            // The element is a TextElement or a Placeable
            trimmed.add(element)
        }

        // Trim trailing whitespace from the Pattern.
        val lastElement = trimmed.last()
        if (lastElement is TextElement) {
            lastElement.value = lastElement.value.replace(trailingWSRe, "")
            if (lastElement.value.isEmpty()) {
                trimmed.removeAt(trimmed.size - 1)
            }
        }

        return trimmed
    }

    private fun getTextElement(ps: FluentStream): TextElement {
        var buffer = ""
        var ch: Char?
        while (true) {
            ch = ps.currentChar()
            if (ch == null) break
            if (ch == '{' || ch == '}' || ch == EOL) {
                return TextElement(buffer)
            }
            buffer += ch
            ps.next()
        }
        return TextElement(buffer)
    }

    private fun getEscapeSequence(ps: FluentStream): String {

        return when (val next = ps.currentChar()) {
            '\\', '"' -> {
                ps.next()
                "\\$next"
            }
            'u' -> this.getUnicodeEscapeSequence(ps, next, 4)
            'U' -> this.getUnicodeEscapeSequence(ps, next, 6)
            else -> throw ParseError("E0025", next ?: "EOF")
        }
    }

    private fun getUnicodeEscapeSequence(
            ps: FluentStream,
            u: Char,
            digits: Int
    ): String {
        ps.expectChar(u)

        var sequence = ""
        for (i in 0 until digits) {
            val ch = ps.takeHexDigit()
                    ?: throw ParseError(
                            "E0026", "\\${u}${sequence}${ps.currentChar()}"
                    )

            sequence += ch
        }

        return "\\${u}$sequence"
    }

    private fun getPlaceable(ps: FluentStream): PatternElement {
        ps.expectChar('{')
        ps.skipBlank()
        val expression = when (ps.currentChar()) {
            '{' -> {
                val child = this.getPlaceable(ps)
                ps.skipBlank()
                child
            }
            else -> this.getExpression(ps)
        }
        ps.expectChar('}')
        return Placeable(expression as InsidePlaceable)
    }

    private fun getExpression(ps: FluentStream): Expression {
        val selector = this.getInlineExpression(ps)
        ps.skipBlank()

        if (ps.currentChar() == '-') {
            if (ps.peek() != '>') {
                ps.resetPeek()
                return selector
            }

            // Validate selector expression according to
            // abstract.js in the Fluent specification

            if (selector is MessageReference) {
                if (selector.attribute == null) {
                    throw ParseError("E0016")
                } else {
                    throw ParseError("E0018")
                }
            } else if (selector is TermReference) {
                if (selector.attribute == null) {
                    throw ParseError("E0017")
                }
            }

            ps.next()
            ps.next()

            ps.skipBlankInline()
            ps.expectLineEnd()

            val variants = this.getVariants(ps)
            return SelectExpression(selector, variants)
        }

        if (selector is TermReference && selector.attribute !== null) {
            throw ParseError("E0019")
        }

        return selector
    }

    private fun getInlineExpression(ps: FluentStream): Expression {
        if (ps.isNumberStart()) {
            return this.getNumber(ps)
        }

        if (ps.currentChar() == '"') {
            return this.getString(ps)
        }

        if (ps.currentChar() == '$') {
            ps.next()
            val id = this.getIdentifier(ps)
            return VariableReference(id)
        }

        if (ps.currentChar() == '-') {
            ps.next()
            val id = this.getIdentifier(ps)

            var attr: Identifier? = null
            if (ps.currentChar() == '.') {
                ps.next()
                attr = this.getIdentifier(ps)
            }

            var args: CallArguments? = null
            ps.peekBlank()
            if (ps.currentPeek() == '(') {
                ps.skipToPeek()
                args = this.getCallArguments(ps)
            }

            return TermReference(id, attr, args)
        }

        if (ps.isIdentifierStart()) {
            val id = this.getIdentifier(ps)
            ps.peekBlank()

            if (ps.currentPeek() == '(') {
                // It's a Function. Ensure it's all upper-case.
                if (VALID_FUNCTION_NAME.matchEntire(id.name) == null) {
                    throw ParseError("E0008")
                }

                ps.skipToPeek()
                val args = this.getCallArguments(ps)
                return FunctionReference(id, args)
            }

            var attr: Identifier? = null
            if (ps.currentChar() == '.') {
                ps.next()
                attr = this.getIdentifier(ps)
            }

            return MessageReference(id, attr)
        }

        throw ParseError("E0028")
    }

    private fun getCallArgument(ps: FluentStream): CallArgument {
        val exp = this.getInlineExpression(ps)

        ps.skipBlank()

        if (ps.currentChar() != ':') {
            return exp
        }

        if (exp is MessageReference && exp.attribute == null) {
            ps.next()
            ps.skipBlank()

            val value = this.getLiteral(ps)
            return NamedArgument(exp.id, value)
        }

        throw ParseError("E0009")
    }

    private fun getCallArguments(ps: FluentStream): CallArguments {
        val positional: MutableList<Expression> = mutableListOf()
        val named: MutableList<NamedArgument> = mutableListOf()
        val argumentNames: MutableSet<String> = mutableSetOf()

        ps.expectChar('(')
        ps.skipBlank()

        while (true) {
            if (ps.currentChar() == ')') {
                break
            }

            when (val arg = this.getCallArgument(ps)) {
                is NamedArgument -> {
                    if (argumentNames.contains(arg.name.name)) {
                        throw ParseError("E0022")
                    }
                    named.add(arg)
                    argumentNames.add(arg.name.name)
                }
                is Expression -> {
                    if (argumentNames.size > 0) {
                        throw ParseError("E0021")
                    }
                    positional.add(arg)
                }
            }

            ps.skipBlank()

            if (ps.currentChar() == ',') {
                ps.next()
                ps.skipBlank()
                continue
            }

            break
        }

        ps.expectChar(')')
        return CallArguments(positional, named)
    }

    private fun getString(ps: FluentStream): StringLiteral {
        ps.expectChar('"')
        var value = ""

        val filter = { x: Char -> x != '"' && x != EOL }
        var ch = ps.takeChar(filter)
        while (ch != null) {
            if (ch == '\\') {
                value += this.getEscapeSequence(ps)
            } else {
                value += ch
            }
            ch = ps.takeChar(filter)
        }

        if (ps.currentChar() == EOL) {
            throw ParseError("E0020")
        }

        ps.expectChar('"')

        return StringLiteral(value)
    }

    private fun getLiteral(ps: FluentStream): Literal {
        if (ps.isNumberStart()) {
            return this.getNumber(ps)
        }

        if (ps.currentChar() == '"') {
            return this.getString(ps)
        }

        throw ParseError("E0014")
    }
}

private class Indent(var value: String, span: Span? = null) : PatternElement(span)
