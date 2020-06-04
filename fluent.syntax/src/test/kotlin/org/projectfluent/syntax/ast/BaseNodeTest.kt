package org.projectfluent.syntax.ast

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.parser.FluentParser

class BaseNodeTest {
    val parser = FluentParser()
    @Test fun test_equals() {
        val m1 = Message(Identifier("test-id"), Pattern(TextElement("localized")))
        assertTrue(m1.nodeEquals(m1))
        val m2 = Message(Identifier("test-id"), Pattern(TextElement("different")))
        assertFalse(m1.nodeEquals(m2))
        assertTrue(m1.nodeEquals(m2, arrayOf("span", "value")))
        assertNotEquals(m1, m2)
        assertEquals(m1.id, m2.id)
        assertFalse(m1.id.nodeEquals(m2.value))
        assertFalse(m1.nodeEquals(null))
        assertNotEquals(m1, null)
        assertNotEquals(null, m1)
    }

    @Test
    fun variant_order() {
        val thisRes = this.parser.parse(
            """
            |msg = { ${'$'}val ->
            |  [few] things
            |  [1] one
            | *[other] default
            |}
        """.trimMargin()
        )
        val otherRes = this.parser.parse(
            """
            |msg = { ${'$'}val ->
            |  [few] things
            | *[other] default
            |  [1] one
            |}
        """.trimMargin()
        )
        assertTrue(thisRes.body[0].nodeEquals(otherRes.body[0], ignoredFields = arrayOf("span", "variants")))
        assertFalse(thisRes.body[0].nodeEquals(otherRes.body[0]))
    }
    @Test
    fun attribute_order() {
        val thisRes = this.parser.parse(
            """
            |msg =
            |  .attr1 = one
            |  .attr2 = two
        """.trimMargin()
        )
        val otherRes = this.parser.parse(
            """
            |msg =
            |  .attr2 = two
            |  .attr1 = one
        """.trimMargin()
        )
        assertTrue(thisRes.body[0].nodeEquals(otherRes.body[0], ignoredFields = arrayOf("span", "attributes")))
        assertFalse(thisRes.body[0].nodeEquals(otherRes.body[0]))
    }
}
