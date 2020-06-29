package org.projectfluent.syntax.parser

import kotlin.math.max
import kotlin.math.min

internal const val EOL = '\n'
internal val EOF: Char? = null

internal open class ParserStream(val string: String) {
    var index: Int = 0
    var peekOffset: Int = 0

    private fun cursorAtCRLF(offset: Int): Boolean {
        val maxIndex = max(string.length - 1, 0)
        val nextTwoChars = string.subSequence(min(offset, maxIndex), min(offset + 2, maxIndex))
        return nextTwoChars.toString() == "\r\n"
    }

    private fun charAt(offset: Int): Char? {
        // When the cursor is at CRLF, return LF but don't move the cursor.
        // The cursor still points to the EOL position, which in this case is the
        // beginning of the compound CRLF sequence. This ensures slices of
        // [inclusive, exclusive) continue to work properly.
        return if (cursorAtCRLF(offset)) {
            EOL
        } else {
            this.string.getOrNull(offset)
        }
    }

    fun currentChar() = charAt(index)

    fun currentPeek() = charAt(index + peekOffset)

    fun next(): Char? {
        resetPeek()
        index += if (cursorAtCRLF(index)) { // Skip over the CRLF as if it was a single character.
            2
        } else {
            1
        }
        return currentChar()
    }

    fun peek(): Char? {
        peekOffset += if (cursorAtCRLF(index + peekOffset)) { // Skip over the CRLF as if it was a single character.
            2
        } else {
            1
        }
        return currentPeek()
    }

    fun resetPeek(offset: Int = 0) {
        this.peekOffset = offset
    }

    fun skipToPeek() {
        this.index += this.peekOffset
        resetPeek()
    }
}

internal class FluentStream(string: String) : ParserStream(string) {

    fun peekBlankInline(): String {
        val start = this.index + this.peekOffset
        while (this.currentPeek() == ' ') {
            this.peek()
        }
        return this.string.slice(start until this.index + this.peekOffset)
    }

    fun skipBlankInline(): String {
        val blank = this.peekBlankInline()
        this.skipToPeek()
        return blank
    }

    fun peekBlankBlock(): String {
        val blank = StringBuilder()
        while (true) {
            val lineStart = this.peekOffset
            this.peekBlankInline()
            when (val current = currentPeek()) {
                EOL -> { // Continue to next line.
                    blank.append(current)
                    peek()
                }
                EOF -> return blank.toString() // Treat the blank line at EOF as a blank block.
                else -> { // Any other char; reset to column 1 on this line.
                    resetPeek(lineStart)
                    return blank.toString()
                }
            }
        }
    }

    fun skipBlankBlock(): String {
        val blank = this.peekBlankBlock()
        this.skipToPeek()
        return blank
    }

    fun peekBlank() {
        while (this.currentPeek() == ' ' || this.currentPeek() == EOL) {
            this.peek()
        }
    }

    fun skipBlank() {
        this.peekBlank()
        this.skipToPeek()
    }

    fun expectChar(ch: Char) =
        when (currentChar()) {
            ch -> next()
            else -> throw ParseError("E0003", ch)
        }

    fun expectLineEnd() =
        when (currentChar()) {
            EOL, EOF -> next() // EOF is a valid line end in Fluent.
            else -> throw ParseError("E0003", "\u2424") // Unicode Character 'SYMBOL FOR NEWLINE' (U+2424)
        }

    fun takeChar(predicate: (ch: Char) -> Boolean): Char? {
        val current = currentChar()
        return if (current != null && predicate(current)) {
            this.next()
            current
        } else {
            null
        }
    }

    fun isIdentifierStart() = isCharIdStart(currentPeek())

    fun isNumberStart(): Boolean {
        val ch = if (currentChar() == '-') {
            peek()
        } else {
            currentChar()
        }
        this.resetPeek()
        return ch?.toInt() in 48..57 // 0-9 (isDigit)
    }

    fun isValueStart() =
        // Inline Patterns may start with any char.
        when (currentPeek()) {
            EOL, EOF -> false
            else -> true
        }

    fun isValueContinuation(): Boolean {
        val column1 = this.peekOffset
        this.peekBlankInline()
        val current = currentPeek()

        return when {
            current == '{' -> {
                resetPeek(column1)
                true
            }
            peekOffset == column1 -> false
            isCharPatternContinuation(current) -> {
                resetPeek(column1)
                true
            }
            else -> false
        }
    }

    // -1 - any
    //  0 - comment
    //  1 - group comment
    //  2 - resource comment
    fun isNextLineComment(level: Int = -1): Boolean {
        if (this.currentChar() != EOL) {
            return false
        }

        val bound = if (level == -1) 2 else level
        for (i in 0..bound) {
            if (this.peek() != '#') {
                if (level != -1) {
                    this.resetPeek()
                    return false
                } else {
                    break
                }
            }
        }

        // The first char after #, ## or ###.
        val ch = peek()
        resetPeek()
        return when (ch) {
            ' ', EOL -> true
            else -> false
        }
    }

    fun isVariantStart(): Boolean {
        val currentPeekOffset = this.peekOffset
        if (this.currentPeek() == '*') {
            this.peek()
        }
        val ret = this.currentPeek() == '['
        this.resetPeek(currentPeekOffset)
        return ret
    }

    fun isAttributeStart() = currentPeek() == '.'

    fun skipToNextEntryStart(junkStart: Int) {
        val lastNewline = this.string.lastIndexOf(EOL, this.index)
        if (junkStart < lastNewline) {
            // Last seen newline is _after_ the junk start. It's safe to rewind
            // without the risk of resuming at the same broken entry.
            this.index = lastNewline
        }
        while (currentChar() != EOF) {
            // We're only interested in beginnings of line.
            if (this.currentChar() != EOL) {
                this.next()
                continue
            }

            // Break if the first char in this line looks like an entry start.
            val first = this.next()
            if (isCharIdStart(first) || first == '-' || first == '#') {
                return
            }
        }
    }

    fun takeIDStart(): Char {
        val current = currentChar()
        return if (current != null && isCharIdStart(current)) {
            this.next()
            current
        } else {
            throw ParseError("E0004", "a-zA-Z")
        }
    }

    fun takeIDChar() =
        takeChar {
            when (it.toInt()) {
                in 97..122, in 65..90, in 48..57, 95, 45 -> true // a-z, A-Z, _, -
                else -> false
            }
        }

    fun takeDigit() =
        takeChar {
            when (it.toInt()) {
                in 48..57 -> true // 0-9
                else -> false
            }
        }

    fun takeHexDigit() =
        takeChar {
            when (it.toInt()) {
                in 48..57, in 65..70, in 97..102 -> true // 0-9, A-F, a-f
                else -> false
            }
        }

    private companion object {
        private const val SPECIAL_LINE_START_CHARS = "}.[*"

        private fun isCharIdStart(ch: Char?) =
            when (ch?.toInt()) {
                in 97..122, in 65..90 -> true // a-z, A-Z
                else -> false
            }

        private fun isCharPatternContinuation(ch: Char?) = (ch != null) && (ch !in SPECIAL_LINE_START_CHARS)
    }
}
