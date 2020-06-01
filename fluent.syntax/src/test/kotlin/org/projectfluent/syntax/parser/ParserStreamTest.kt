package org.projectfluent.syntax.parser

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ParserStreamTest {

    @Test
    fun next() {
        val ps = ParserStream("abcd")
        assertEquals('a', ps.currentChar())
        assertEquals(0, ps.index)

        assertEquals('b', ps.next())
        assertEquals('b', ps.currentChar())
        assertEquals(1, ps.index)

        assertEquals('c', ps.next())
        assertEquals('c', ps.currentChar())
        assertEquals(2, ps.index)

        assertEquals('d', ps.next())
        assertEquals('d', ps.currentChar())
        assertEquals(3, ps.index)

        assertEquals(null, ps.next())
        assertEquals(null, ps.currentChar())
        assertEquals(4, ps.index)
    }

    @Test
    fun peek() {
        val ps = ParserStream("abcd")
        assertEquals('a', ps.currentPeek())
        assertEquals(0, ps.peekOffset)

        assertEquals('b', ps.peek())
        assertEquals('b', ps.currentPeek())
        assertEquals(1, ps.peekOffset)

        assertEquals('c', ps.peek())
        assertEquals('c', ps.currentPeek())
        assertEquals(2, ps.peekOffset)

        assertEquals('d', ps.peek())
        assertEquals('d', ps.currentPeek())
        assertEquals(3, ps.peekOffset)

        assertEquals(null, ps.peek())
        assertEquals(null, ps.currentPeek())
        assertEquals(4, ps.peekOffset)
    }

    @Test
    fun peek_and_next() {
        val ps = ParserStream("abcd")

        assertEquals('b', ps.peek());
        assertEquals(1, ps.peekOffset);
        assertEquals(0, ps.index);

        assertEquals('b', ps.next());
        assertEquals(0, ps.peekOffset);
        assertEquals(1, ps.index);

        assertEquals('c', ps.peek());
        assertEquals(1, ps.peekOffset);
        assertEquals(1, ps.index);

        assertEquals('c', ps.next());
        assertEquals(0, ps.peekOffset);
        assertEquals(2, ps.index);
        assertEquals('c', ps.currentChar());
        assertEquals('c', ps.currentPeek());

        assertEquals('d', ps.peek());
        assertEquals(1, ps.peekOffset);
        assertEquals(2, ps.index);

        assertEquals('d', ps.next());
        assertEquals(0, ps.peekOffset);
        assertEquals(3, ps.index);
        assertEquals('d', ps.currentChar());
        assertEquals('d', ps.currentPeek());

        assertEquals(null, ps.peek());
        assertEquals(1, ps.peekOffset);
        assertEquals(3, ps.index);
        assertEquals('d', ps.currentChar());
        assertEquals(null, ps.currentPeek());

        assertEquals(null, ps.peek());
        assertEquals(2, ps.peekOffset);
        assertEquals(3, ps.index);

        assertEquals(null, ps.next());
        assertEquals(0, ps.peekOffset);
        assertEquals(4, ps.index);
    }

    @Test
    fun skip_to_peek() {
        val ps = ParserStream("abcd")

        ps.peek();
        ps.peek();

        ps.skipToPeek();

        assertEquals('c', ps.currentChar());
        assertEquals('c', ps.currentPeek());
        assertEquals(0, ps.peekOffset);
        assertEquals(2, ps.index);

        ps.peek();

        assertEquals('c', ps.currentChar());
        assertEquals('d', ps.currentPeek());
        assertEquals(1, ps.peekOffset);
        assertEquals(2, ps.index);

        ps.next();

        assertEquals('d', ps.currentChar());
        assertEquals('d', ps.currentPeek());
        assertEquals(0, ps.peekOffset);
        assertEquals(3, ps.index);
    }

    @Test
    fun reset_peek() {
        val ps = ParserStream("abcd")

        ps.next();
        ps.peek();
        ps.peek();
        ps.resetPeek();

        assertEquals('b', ps.currentChar());
        assertEquals('b', ps.currentPeek());
        assertEquals(0, ps.peekOffset);
        assertEquals(1, ps.index);

        ps.peek();

        assertEquals('b', ps.currentChar());
        assertEquals('c', ps.currentPeek());
        assertEquals(1, ps.peekOffset);
        assertEquals(1, ps.index);

        ps.peek();
        ps.peek();
        ps.peek();
        ps.resetPeek();

        assertEquals('b', ps.currentChar());
        assertEquals('b', ps.currentPeek());
        assertEquals(0, ps.peekOffset);
        assertEquals(1, ps.index);

        assertEquals('c', ps.peek());
        assertEquals('b', ps.currentChar());
        assertEquals('c', ps.currentPeek());
        assertEquals(1, ps.peekOffset);
        assertEquals(1, ps.index);

        assertEquals('d', ps.peek());
        assertEquals(null, ps.peek());
    }
}
