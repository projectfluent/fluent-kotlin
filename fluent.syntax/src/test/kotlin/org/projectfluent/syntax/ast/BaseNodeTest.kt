package org.projectfluent.syntax.ast


import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class BaseNodeTest {
    @Test fun test_equals() {
        val m1 = Message(Identifier("test-id"), Pattern(TextElement("localized")))
        assertTrue(m1.equals(m1))
        val m2 = Message(Identifier("test-id"), Pattern(TextElement("different")))
        assertFalse(m1.equals(m2))
    }
}