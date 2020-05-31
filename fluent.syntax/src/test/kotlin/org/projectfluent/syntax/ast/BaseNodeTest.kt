package org.projectfluent.syntax.ast

import org.junit.Assert.*
import kotlin.test.Test

class BaseNodeTest {
    @Test fun test_equals() {
        val m1 = Message("test-id", "localized")
        assertTrue(m1.equals(m1))
        val m2 = Message("test-id", "different")
        assertFalse(m1.equals(m2))
    }
}