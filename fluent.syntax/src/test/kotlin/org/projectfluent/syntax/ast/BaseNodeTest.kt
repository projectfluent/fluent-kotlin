package org.projectfluent.syntax.ast

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.parser.FluentParser

class BaseNodeTest {
    private val parser = FluentParser()

    @Test
    fun publicProperties() {
        val variant = Variant(Identifier("other"), Pattern(), true)

        assertEquals(
            setOf("default", "key", "span", "value"),
            variant.properties().map { it.first }.toSet()
        )
    }

    @Test
    fun testEquals() {
        val m1 = Message(Identifier("test-id"), Pattern(TextElement("localized")))
        val m2 = Message(Identifier("test-id"), Pattern(TextElement("different")))

        assertEquals(m1, m1)
        assertNotEquals(m1, m2)
        assertEquals(m1.id, m2.id)
        assertNotEquals(m1.value, m2.value)
        assertNotEquals(m1, null)
        assertNotEquals(null, m1)
    }

    @Test
    fun testEqualsWithIgnoredFields() {
        val m1 = Message(Identifier("test-id"), Pattern(TextElement("localized")))
        val m2 = Message(Identifier("test-id"), Pattern(TextElement("different")))

        assertTrue(m1.equals(m2, setOf("span", "value")))
    }

    @Test
    fun variantOrderIsImportantForEquals() {
        val resource1 = this.parser.parse(
            """
            |msg = { ${'$'}val ->
            |  [few] things
            |  [1] one
            | *[other] default
            |}
        """.trimMargin()
        )
        val resource2 = this.parser.parse(
            """
            |msg = { ${'$'}val ->
            |  [few] things
            | *[other] default
            |  [1] one
            |}
        """.trimMargin()
        )

        assertTrue(resource1.body[0].equals(resource2.body[0], ignoredFields = setOf("span", "variants")))
        assertNotEquals(resource1.body[0], resource2.body[0])
    }

    @Test
    fun attributeOrderIsImportantForEquals() {
        val resource1 = this.parser.parse(
            """
            |msg =
            |  .attr1 = one
            |  .attr2 = two
        """.trimMargin()
        )
        val resource2 = this.parser.parse(
            """
            |msg =
            |  .attr2 = two
            |  .attr1 = one
        """.trimMargin()
        )

        assertTrue(resource1.body[0].equals(resource2.body[0], ignoredFields = setOf("span", "attributes")))
        assertNotEquals(resource1.body[0], resource2.body[0])
    }
}
