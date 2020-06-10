package org.projectfluent.syntax.serializer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.ast.Entry
import org.projectfluent.syntax.ast.Expression
import org.projectfluent.syntax.ast.Message
import org.projectfluent.syntax.ast.Placeable

class SerializeExpressionTest : SerializerTest() {

    override fun pretty(input: String): String {
        val resource = this.parser.parse(input)
        val first = resource.body[0]
        if (first is Message) {
            val element = first.value?.elements?.get(0)
            if (element is Placeable) {
                val expr = element.expression as Expression
                val serialized = this.serializer.serialize(expr)
                return serialized.toString()
            }
        }
        throw Exception("""
            The first entry of the resource must be a message and the first element
            of its value must be a placeable with an expression.
        """.trimIndent())
    }

    @Test
    fun string_literal() {
        val input = """
            foo = { "str" }
            
        """.trimIndent()
        assertEquals("\"str\"", this.pretty(input))
    }

    @Test
    fun number_literal() {
        val input = """
            foo = { 3 }
            
        """.trimIndent()
        assertEquals("3", this.pretty(input))
    }

    @Test
    fun message_reference() {
        val input = """
            foo = { msg }
            
        """.trimIndent()
        assertEquals("msg", this.pretty(input))
    }

    @Test
    fun message_attribute_reference() {
        val input = """
            foo = { msg.attr }
            
        """.trimIndent()
        assertEquals("msg.attr", this.pretty(input))
    }

    @Test
    fun variable_reference() {
        val input = """
            foo = { ${'$'}var }
            
        """.trimIndent()
        assertEquals("\$var", this.pretty(input))
    }

    @Test
    fun call_expression() {
        val input = """
            foo = { BUILTIN(3.14, kwarg: "value") }
            
        """.trimIndent()
        assertEquals("BUILTIN(3.14, kwarg: \"value\")", this.pretty(input))
    }

    @Test
    fun select_expression() {
        val input = """
            foo =
                { ${'$'}num ->
                   *[one] One
                }
            
        """.trimIndent()
        assertEquals("\$num ->\n   *[one] One\n", this.pretty(input))
    }
}
