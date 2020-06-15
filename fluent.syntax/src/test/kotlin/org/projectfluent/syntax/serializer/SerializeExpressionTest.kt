package org.projectfluent.syntax.serializer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.ast.*
import org.projectfluent.syntax.parser.FluentParser

class SerializeExpressionTest {
    private val parser = FluentParser()
    private val serializer = FluentSerializer()

    private fun parse(input: String): Expression {
        val resource = this.parser.parse(input)
        val first = resource.body[0] as Message
        val element = first.value?.elements?.get(0) as Placeable
        return element.expression as Expression
    }

    private fun pretty(input: String): String {
        val first = this.parse(input)
        val serialized = this.serializer.serialize(first)
        return serialized.toString()
    }

    @Test
    fun string_literal() {
        val input =
            """
            foo = { "str" }
            
            """.trimIndent()
        assertEquals("\"str\"", this.pretty(input))
    }

    @Test
    fun number_literal() {
        val input =
            """
            foo = { 3 }
            
            """.trimIndent()
        assertEquals("3", this.pretty(input))
    }

    @Test
    fun number_literal_type() {
        val key = NumberLiteral("1")
        assertEquals("1", this.serializer.serialize(key as Expression))
    }

    @Test
    fun message_reference() {
        val input =
            """
            foo = { msg }
            
            """.trimIndent()
        assertEquals("msg", this.pretty(input))
    }

    @Test
    fun message_attribute_reference() {
        val input =
            """
            foo = { msg.attr }
            
            """.trimIndent()
        assertEquals("msg.attr", this.pretty(input))
    }

    @Test
    fun variable_reference() {
        val input =
            """
            foo = { ${'$'}var }
            
            """.trimIndent()
        assertEquals("\$var", this.pretty(input))
    }

    @Test
    fun call_expression() {
        val input =
            """
            foo = { BUILTIN(3.14, kwarg: "value") }
            
            """.trimIndent()
        assertEquals("BUILTIN(3.14, kwarg: \"value\")", this.pretty(input))
    }

    @Test
    fun select_expression() {
        val input =
            """
            foo =
                { ${'$'}num ->
                   *[one] One
                }
            
            """.trimIndent()
        assertEquals("\$num ->\n   *[one] One\n", this.pretty(input))
    }
}
