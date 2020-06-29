package org.projectfluent.syntax.parser

import kotlin.math.max
import kotlin.math.min

internal const val EOL = '\n'
internal val EOF: Char? = null
private const val SPECIAL_LINE_START_CHARS = "}.[*"

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
        var blank = ""
        while (true) {
            val lineStart = this.peekOffset
            this.peekBlankInline()
            if (this.currentPeek() == EOL) {
                blank += EOL
                this.peek()
                continue
            }
            if (this.currentPeek() == EOF) {
                // Treat the blank line at EOF as a blank block.
                return blank
            }
            // Any other char; reset to column 1 on this line.
            this.resetPeek(lineStart)
            return blank
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

    fun expectChar(ch: Char) {
        if (this.currentChar() == ch) {
            this.next()
            return
        }

        throw ParseError("E0003", ch)
    }

    fun expectLineEnd() {
        if (this.currentChar() == EOF) {
            // EOF is a valid line end in Fluent.
            return
        }

        if (this.currentChar() == EOL) {
            this.next()
            return
        }

        // Unicode Character 'SYMBOL FOR NEWLINE' (U+2424)
        throw ParseError("E0003", "\u2424")
    }

    fun takeChar(f: (ch: Char) -> Boolean): Char? {
        val ch = this.currentChar()
        ch?.let {
            if (f(ch)) {
                this.next()
                return ch
            }
        }
        return null
    }

    private fun isCharIdStart(ch: Char?): Boolean {
        ch?.let {
            val cc = ch.toInt()
            return (cc in 97..122) || // a-z
                (cc in 65..90) // A-Z
        }
        return false
    }

    fun isIdentifierStart(): Boolean {
        return this.isCharIdStart(this.currentPeek())
    }

    fun isNumberStart(): Boolean {
        val ch = if (this.currentChar() == '-') this.peek() else this.currentChar()

        ch?.let {
            val cc = ch.toInt()
            val isDigit = cc in 48..57 // 0-9
            this.resetPeek()
            return isDigit
        }
        this.resetPeek()
        return false
    }

    private fun isCharPatternContinuation(ch: Char?): Boolean {
        ch?.let {
            return SPECIAL_LINE_START_CHARS.indexOf(ch) < 0
        }
        return false
    }

    fun isValueStart(): Boolean {
        // Inline Patterns may start with any char.
        val ch = this.currentPeek()
        return ch != EOL && ch != EOF
    }

    fun isValueContinuation(): Boolean {
        val column1 = this.peekOffset
        this.peekBlankInline()

        if (this.currentPeek() == '{') {
            this.resetPeek(column1)
            return true
        }

        if (this.peekOffset - column1 == 0) {
            return false
        }

        if (this.isCharPatternContinuation(this.currentPeek())) {
            this.resetPeek(column1)
            return true
        }

        return false
    }

    // -1 - any
    //  0 - comment
    //  1 - group comment
    //  2 - resource comment
    fun isNextLineComment(level: Int = -1): Boolean {
        if (this.currentChar() != EOL) {
            return false
        }

        var i = 0

        while (i <= level || (level == -1 && i < 3)) {
            if (this.peek() != '#') {
                if (i <= level && level != -1) {
                    this.resetPeek()
                    return false
                }
                break
            }
            i++
        }

        // The first char after #, ## or ###.
        val ch = this.peek()
        if (ch == ' ' || ch == EOL) {
            this.resetPeek()
            return true
        }

        this.resetPeek()
        return false
    }

    fun isVariantStart(): Boolean {
        val currentPeekOffset = this.peekOffset
        if (this.currentPeek() == '*') {
            this.peek()
        }
        if (this.currentPeek() == '[') {
            this.resetPeek(currentPeekOffset)
            return true
        }
        this.resetPeek(currentPeekOffset)
        return false
    }

    fun isAttributeStart(): Boolean {
        return this.currentPeek() == '.'
    }

    fun skipToNextEntryStart(junkStart: Int) {
        val lastNewline = this.string.lastIndexOf(EOL, this.index)
        if (junkStart < lastNewline) {
            // Last seen newline is _after_ the junk start. It's safe to rewind
            // without the risk of resuming at the same broken entry.
            this.index = lastNewline
        }
        while (this.currentChar() != null) {
            // We're only interested in beginnings of line.
            if (this.currentChar() != EOL) {
                this.next()
                continue
            }

            // Break if the first char in this line looks like an entry start.
            val first = this.next()
            if (this.isCharIdStart(first) || first == '-' || first == '#') {
                break
            }
        }
    }

    fun takeIDStart(): Char {
        if (this.isCharIdStart(this.currentChar())) {
            val ret = this.currentChar()
            // Kotlin doesn't know we're non-null, make it so
            ret?.let {
                this.next()
                return ret
            }
        }

        throw ParseError("E0004", "a-zA-Z")
    }

    fun takeIDChar(): Char? {
        val closure = fun(ch: Char): Boolean {
            val cc = ch.toInt()
            return (
                (cc in 97..122) || // a-z
                    (cc in 65..90) || // A-Z
                    (cc in 48..57) || // 0-9
                    cc == 95 || cc == 45
                ) // _-
        }

        return this.takeChar(closure)
    }

    fun takeDigit(): Char? {
        val closure = fun(ch: Char): Boolean {
            val cc = ch.toInt()
            return (cc in 48..57) // 0-9
        }

        return this.takeChar(closure)
    }

    fun takeHexDigit(): Char? {
        val closure = fun(ch: Char): Boolean {
            val cc = ch.toInt()
            return (cc in 48..57) || // 0-9
                (cc in 65..70) || // A-F
                (cc in 97..102) // a-f
        }

        return this.takeChar(closure)
    }
}
