package org.projectfluent.syntax.serializer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.ast.*
import org.projectfluent.syntax.parser.FluentParser

class SerializeVariantKeyTest {
    private val parser = FluentParser()
    private val serializer = FluentSerializer()

    private fun parse(input: String): SelectExpression {
        val resource = this.parser.parse(input)
        val first = resource.body[0] as Message
        val element = first.value?.elements?.get(0) as Placeable
        return element.expression as SelectExpression
    }

    private fun pretty_key(input: String, index: Int): String {
        val key = this.parse(input).variants[index].key
        val serialized = this.serializer.serialize(key)
        return serialized.toString()
    }

    @Test
    fun identifier_key() {
        val input =
            """
            foo = { 0 ->
                [one] One
               *[other] Other
            }
            
            """.trimIndent()
        assertEquals("one", this.pretty_key(input, 0))
        assertEquals("other", this.pretty_key(input, 1))
    }

    @Test
    fun number_key() {
        val input =
            """
            foo = { 0 ->
                [-123456789] Minus a lot
                [0] Zero
               *[3.14] Pi
                [007] James
            }
            
            """.trimIndent()
        assertEquals("-123456789", this.pretty_key(input, 0))
        assertEquals("0", this.pretty_key(input, 1))
        assertEquals("3.14", this.pretty_key(input, 2))
        assertEquals("007", this.pretty_key(input, 3))
    }

    @Test
    fun number_literal_type() {
        val key = NumberLiteral("1")
        assertEquals("1", this.serializer.serialize(key as VariantKey))
    }
}
