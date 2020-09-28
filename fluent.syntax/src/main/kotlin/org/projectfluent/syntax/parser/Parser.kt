package org.projectfluent.syntax.parser

import org.projectfluent.syntax.ast.* // ktlint-disable no-wildcard-imports

private val trailingWSRe = Regex("[ \t\n\r]+\$")
private val VALID_FUNCTION_NAME = Regex("^[A-Z][A-Z0-9_-]*\$")

/**
 * Parse Fluent resources.
 *
 * @property withSpans create source positions for nodes (not supported yet)
 */
class FluentParser(private val withSpans: Boolean = false) {

    fun parse(source: String): Resource {
        val ps = FluentStream(source)
        val entries: MutableList<TopLevel> = mutableListOf()
        ps.skipBlankBlock()
            .takeIf { it.isNotEmpty() }
            ?.also { entries.add(Whitespace(it)) }

        var lastComment: Comment? = null
        while (ps.currentChar() != null) {
            val entry = this.getEntryOrJunk(ps)
            val blankLines = ps.skipBlankBlock()

            // Regular Comments require special logic. Comments may be attached to
            // Messages or Terms if they are followed immediately by them. However
            // they should parse as standalone when they're followed by Junk.
            // Consequently, we only attach Comments once we know that the Message
            // or the Term parsed successfully.
            if (entry is Comment && blankLines.isEmpty() && ps.currentChar() != EOF) {
                // Stash the comment and decide what to do with it in the next pass.
                lastComment = entry
                continue
            }

            if (lastComment != null) {
                when (entry) {
                    is Message -> entry.comment = lastComment
                    is Term -> entry.comment = lastComment
                    else -> entries.add(lastComment)
                }
                // In either case, the stashed comment has been dealt with; clear it.
                lastComment = null
            }

            // No special logic for other types of entries.
            entries.add(entry)
            blankLines
                .takeIf { it.isNotEmpty() }
                ?.also { entries.add(Whitespace(it)) }
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
            return Junk(ps.string.substring(entryStartPos, nextEntryStart))
                .also {
                    it.annotations.add(
                        Annotation(err.code, err.message ?: "", err.args, Span(errorIndex, errorIndex))
                    )
                }
        }
    }

    private fun getEntry(ps: FluentStream): Entry =
        when {
            ps.currentChar() == '#' -> getComment(ps)
            ps.currentChar() == '-' -> getTerm(ps)
            ps.isIdentifierStart() -> getMessage(ps)
            else -> throw ParseError("E0002")
        }

    private fun getComment(ps: FluentStream): BaseComment {
        // 0 - comment
        // 1 - group comment
        // 2 - resource comment
        var level = -1
        val content = StringBuilder()

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
                while (true) {
                    val ch = ps.takeChar { it != EOL }
                    if (ch != EOF) {
                        content.append(ch)
                    } else {
                        break
                    }
                }
            }

            if (ps.isNextLineComment(level)) {
                content.append(ps.currentChar())
                ps.next()
            } else {
                break
            }
        }

        return content.toString().let {
            when (level) {
                0 -> Comment(it)
                1 -> GroupComment(it)
                else -> ResourceComment(it)
            }
        }
    }

    private fun getMessage(ps: FluentStream): Message {
        val id = this.getIdentifier(ps)

        ps.skipBlankInline()
        ps.expectChar('=')

        val value = this.maybeGetPattern(ps)
        val attrs = this.getAttributes(ps)

        return if (value != null || attrs.isNotEmpty()) {
            Message(id, value).also { it.attributes.addAll(attrs) }
        } else {
            throw ParseError("E0005", id.name)
        }
    }

    private fun getTerm(ps: FluentStream): Term {
        ps.expectChar('-')
        val id = this.getIdentifier(ps)

        ps.skipBlankInline()
        ps.expectChar('=')

        val value = this.maybeGetPattern(ps) ?: throw ParseError("E0006", id.name)
        val attrs = this.getAttributes(ps)

        return Term(id, value).also { it.attributes.addAll(attrs) }
    }

    private fun getAttribute(ps: FluentStream): Attribute {
        ps.expectChar('.')
        val key = this.getIdentifier(ps)

        ps.skipBlankInline()
        ps.expectChar('=')

        val value = this.maybeGetPattern(ps) ?: throw ParseError("E0012")

        return Attribute(key, value)
    }

    private fun getAttributes(ps: FluentStream): Collection<Attribute> {
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
        val name = StringBuilder()
        name.append(ps.takeIDStart())
        while (true) {
            val ch = ps.takeIDChar()
            if (ch != EOF) {
                name.append(ch)
            } else {
                break
            }
        }
        return Identifier(name.toString())
    }

    private fun getVariantKey(ps: FluentStream): VariantKey =
        when (ps.currentChar()?.toInt()) {
            null -> throw ParseError("E0013")
            in 48..57, 45 -> getNumber(ps) // 0-9, -
            else -> getIdentifier(ps)
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

        return when {
            variants.size == 0 -> throw ParseError("E0011")
            !hasDefault -> throw ParseError("E0010")
            else -> variants
        }
    }

    private fun getDigits(ps: FluentStream): String {
        val num = StringBuilder()
        while (true) {
            val ch = ps.takeDigit()
            if (ch != EOF) {
                num.append(ch)
            } else {
                break
            }
        }
        return when {
            num.isNotEmpty() -> num.toString()
            else -> throw ParseError("E0004", "0-9")
        }
    }

    private fun getNumber(ps: FluentStream): NumberLiteral {
        val value = StringBuilder()

        if (ps.currentChar() == '-') {
            ps.next()
            value.append("-")
        }
        value.append(getDigits(ps))

        if (ps.currentChar() == '.') {
            ps.next()
            value.append(".", getDigits(ps))
        }

        return NumberLiteral(value.toString())
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

        elements@ while (true) {
            when (ps.currentChar()) {
                EOL -> {
                    val blankStart = ps.index
                    val blankLines = ps.peekBlankBlock()
                    if (ps.isValueContinuation()) {
                        ps.skipToPeek()
                        val indent = ps.skipBlankInline()
                        commonIndentLength = minOf(commonIndentLength, indent.length)
                        elements.add(this.getIndent(ps, blankLines + indent, blankStart))
                        continue@elements
                    } else {
                        // The end condition for getPattern's while loop is a newline
                        // which is not followed by a valid pattern continuation.
                        ps.resetPeek()
                        break@elements
                    }
                }
                '{' -> {
                    elements.add(this.getPlaceable(ps))
                    continue@elements
                }
                '}' -> throw ParseError("E0027")
                EOF -> break@elements
                else -> elements.add(this.getTextElement(ps))
            }
        }

        val dedented = this.dedent(elements, commonIndentLength)
        return Pattern(*dedented.toTypedArray())
    }

    // Create a token representing an indent. It's not part of the AST and it will
    // be trimmed and merged into adjacent TextElements, or turned into a new
    // TextElement, if it's surrounded by two Placeables.
    private fun getIndent(ps: FluentStream, value: String, start: Int) = Indent(value, start, ps.index)

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
                element.value = element.value.slice(0 until (element.value.length - commonIndent))
                if (element.value.isEmpty()) {
                    continue
                }
            }

            if (trimmed.isNotEmpty()) {
                val prev = trimmed.last()
                if (prev is TextElement) {
                    // Join adjacent TextElements by replacing them with their sum.
                    val sum = TextElement(
                        prev.value + when (element) {
                            is TextElement -> element.value
                            is Indent -> element.value
                            else -> throw IllegalStateException("Unexpected PatternElement type")
                        }
                    )
                    if (this.withSpans) {
                        val start = prev.span?.start
                        val end = element.span?.end
                        if (start != null && end != null) sum.addSpan(start, end)
                    }
                    trimmed[trimmed.lastIndex] = sum
                    continue
                }
            }

            if (element is Indent) {
                // If the indent hasn't been merged into a preceding TextElement,
                // convert it into a new TextElement.
                val textElement = TextElement(element.value)
                if (this.withSpans) {
                    val start = element.span?.start
                    val end = element.span?.end
                    if (start != null && end != null) textElement.addSpan(start, end)
                }
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
                trimmed.removeAt(trimmed.lastIndex)
            }
        }

        return trimmed
    }

    private fun getTextElement(ps: FluentStream): TextElement {
        val buffer = StringBuilder()
        while (true) {
            when (val ch = ps.currentChar()) {
                '{', '}', EOL, null -> return TextElement(buffer.toString())
                else -> {
                    buffer.append(ch)
                    ps.next()
                }
            }
        }
    }

    private fun getEscapeSequence(ps: FluentStream) =
        when (val next = ps.currentChar()) {
            '\\', '"' -> {
                ps.next()
                "\\$next"
            }
            'u' -> this.getUnicodeEscapeSequence(ps, next, 4)
            'U' -> this.getUnicodeEscapeSequence(ps, next, 6)
            else -> throw ParseError("E0025", next ?: "EOF")
        }

    private fun getUnicodeEscapeSequence(ps: FluentStream, u: Char, digits: Int): String {
        ps.expectChar(u)
        val sequence = StringBuilder()
        repeat(digits) {
            sequence.append(
                ps.takeHexDigit() ?: throw ParseError("E0026", "\\${u}${sequence}${ps.currentChar()}")
            )
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

        return when {
            ps.currentChar() == '-' && ps.peek() != '>' -> {
                ps.resetPeek()
                selector
            }
            ps.currentChar() == '-' -> {
                // Validate selector expression according to
                // abstract.js in the Fluent specification

                when (selector) {
                    is MessageReference -> when (selector.attribute) {
                        null -> throw ParseError("E0016")
                        else -> throw ParseError("E0018")
                    }
                    is TermReference -> when (selector.attribute) {
                        null -> throw ParseError("E0017")
                    }
                }

                ps.next()
                ps.next()

                ps.skipBlankInline()
                ps.expectLineEnd()

                SelectExpression(selector, getVariants(ps))
            }
            selector is TermReference && selector.attribute !== null -> throw ParseError("E0019")
            else -> selector
        }
    }

    private fun getInlineExpression(ps: FluentStream): Expression {
        return when {
            ps.isNumberStart() -> getNumber(ps)
            ps.currentChar() == '"' -> getString(ps)
            ps.currentChar() == '$' -> {
                ps.next()
                VariableReference(getIdentifier(ps))
            }
            ps.currentChar() == '-' -> {
                ps.next()
                val id = this.getIdentifier(ps)

                val attr: Identifier? = if (ps.currentChar() == '.') {
                    ps.next()
                    getIdentifier(ps)
                } else {
                    null
                }

                ps.peekBlank()
                val args: CallArguments? = if (ps.currentPeek() == '(') {
                    ps.skipToPeek()
                    getCallArguments(ps)
                } else {
                    null
                }

                TermReference(id, attr, args)
            }
            ps.isIdentifierStart() -> {
                val id = this.getIdentifier(ps)
                ps.peekBlank()

                if (ps.currentPeek() == '(') {
                    // It's a Function. Ensure it's all upper-case.
                    VALID_FUNCTION_NAME.matchEntire(id.name) ?: throw ParseError("E0008")
                    ps.skipToPeek()
                    val args = this.getCallArguments(ps)
                    FunctionReference(id, args)
                } else {
                    val attr: Identifier? = if (ps.currentChar() == '.') {
                        ps.next()
                        this.getIdentifier(ps)
                    } else {
                        null
                    }
                    MessageReference(id, attr)
                }
            }
            else -> throw ParseError("E0028")
        }
    }

    private fun getCallArgument(ps: FluentStream): CallArgument {
        val exp = this.getInlineExpression(ps)
        ps.skipBlank()
        return when {
            ps.currentChar() != ':' -> exp
            exp is MessageReference && exp.attribute == null -> {
                ps.next()
                ps.skipBlank()
                NamedArgument(exp.id, getLiteral(ps))
            }
            else -> throw ParseError("E0009")
        }
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
                is NamedArgument ->
                    if (arg.name.name !in argumentNames) {
                        named.add(arg)
                        argumentNames.add(arg.name.name)
                    } else {
                        throw ParseError("E0022")
                    }
                is Expression ->
                    if (argumentNames.isEmpty()) {
                        positional.add(arg)
                    } else {
                        throw ParseError("E0021")
                    }
            }

            ps.skipBlank()

            if (ps.currentChar() == ',') {
                ps.next()
                ps.skipBlank()
                continue
            } else {
                break
            }
        }

        ps.expectChar(')')
        return CallArguments(positional, named)
    }

    private fun getString(ps: FluentStream): StringLiteral {
        ps.expectChar('"')
        val value = StringBuilder()

        while (true) {
            val ch = ps.takeChar { x: Char -> x != '"' && x != EOL }
            if (ch != EOF) {
                value.append(
                    if (ch == '\\') {
                        getEscapeSequence(ps)
                    } else {
                        ch
                    }
                )
            } else {
                break
            }
        }

        return when {
            ps.currentChar() != EOL -> {
                ps.expectChar('"')
                StringLiteral(value.toString())
            }
            else -> throw ParseError("E0020")
        }
    }

    private fun getLiteral(ps: FluentStream) =
        when {
            ps.isNumberStart() -> getNumber(ps)
            ps.currentChar() == '"' -> getString(ps)
            else -> throw ParseError("E0014")
        }
}

private data class Indent(var value: String) : PatternElement() {
    constructor(value: String, start: Int, end: Int) : this(value) {
        this.addSpan(start, end)
    }
}
