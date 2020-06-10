package org.projectfluent.syntax.serializer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.parser.FluentParser

abstract class SerializerTest {
    private val parser = FluentParser()
    private val serializer = FluentSerializer()

    fun pretty(ftl: String): String {
        val resource = this.parser.parse(ftl)
        val serialized = this.serializer.serialize(resource)
        return serialized.toString()
    }
}

class SerializeResourceTest : SerializerTest() {

    @Test
    fun simple_message_without_eol() {
        val input = "foo = Foo"
        assertEquals("foo = Foo\n", this.pretty(input))
    }

    @Test
    fun simple_message() {
        val input = """
            foo = Foo
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun simple_term() {
        val input = """
            -foo = Foo
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun two_simple_messages() {
        val input = """
            foo = Foo
            bar = Bar
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun block_multiline_message() {
        val input = """
            foo =
                Foo
                Bar
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun inline_multiline_message() {
        val input = """
            foo = Foo
                Bar
            
        """.trimIndent()
        val expected = """
            foo =
                Foo
                Bar
            
        """.trimIndent()
        assertEquals(expected, this.pretty(input))
    }

    @Test
    fun message_reference() {
        val input = """
            foo = Foo { bar }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun term_reference() {
        val input = """
            foo = Foo { -bar }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun variable_reference() {
        val input = """
            foo = Foo { ${'$'}bar }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun number_literal() {
        val input = """
            foo = Foo { 1 }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun string_literal() {
        val input = """
            foo = Foo { "bar" }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun message_reference_with_attribute() {
        val input = """
            foo = Foo { bar.baz }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun resource_comment() {
        val input = """
            ### A multiline
            ### resource comment.
            
            foo = Foo
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun group_comment() {
        val input = """
            foo = Foo
            
            ## Comment Header
            ##
            ## A multiline
            ## group comment
            
            bar = Bar
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun message_comment() {
        val input = """
            # A multiline
            # message comment.
            foo = Foo
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun standalone_comment() {
        val input = """
            foo = Foo
            
            # A standalone comment
            
            bar = Bar
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun multiline_with_placeable() {
        val input = """
            foo =
                Foo { bar }
                Baz
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun attribute() {
        val input = """
            foo =
                .attr = Foo Attr
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun multiline_attribute() {
        val input = """
            foo =
                .attr =
                    Foo Attr
                    Continued
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun two_attribute() {
        val input = """
            foo =
                .attr-a = Foo Attr A
                .attr-b = Foo Attr B
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun value_and_attributes() {
        val input = """
            foo = Foo Value
                .attr-a = Foo Attr A
                .attr-b = Foo Attr B
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun multiline_value_and_attributes() {
        val input = """
            foo =
                Foo Value
                Continued
                .attr-a = Foo Attr A
                .attr-b = Foo Attr B
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun select_expression() {
        val input = """
            foo =
                { ${'$'}sel ->
                   *[a] A
                    [b] B
                }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun multiline_variant() {
        val input = """
            foo =
                { ${'$'}sel ->
                   *[a]
                        AAA
                        BBB
                }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun multiline_variant_with_first_line_inline() {
        val input = """
            foo =
                { ${'$'}sel ->
                   *[a] AAA
                        BBB
                }
            
        """.trimIndent()
        val expected = """
            foo =
                { ${'$'}sel ->
                   *[a]
                        AAA
                        BBB
                }
            
        """.trimIndent()
        assertEquals(expected, this.pretty(input))
    }

    @Test
    fun variant_key_number() {
        val input = """
            foo =
                { ${'$'}sel ->
                   *[1] 1
                }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun select_expression_in_block_pattern() {
        val input = """
            foo =
                Foo { ${'$'}sel ->
                   *[a] A
                    [b] B
                }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun select_expression_in_inline_pattern() {
        val input = """
            foo = Foo { ${'$'}sel ->
                   *[a] A
                    [b] B
                }
            
        """.trimIndent()
        val expected = """
            foo =
                Foo { ${'$'}sel ->
                   *[a] A
                    [b] B
                }
            
        """.trimIndent()
        assertEquals(expected, this.pretty(input))
    }

    @Test
    fun select_expression_in_multiline_pattern() {
        val input = """
            foo =
                Foo
                Bar { ${'$'}sel ->
                   *[a] A
                    [b] B
                }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun nested_select_expression() {
        val input = """
            foo =
                { ${'$'}a ->
                   *[a]
                        { ${'$'}b ->
                           *[b] Foo
                        }
                }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun selector_variable_reference() {
        val input = """
            foo =
                { ${'$'}bar ->
                   *[a] A
                }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun selector_number_literal() {
        val input = """
            foo =
                { 1 ->
                   *[a] A
                }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun selector_string_literal() {
        val input = """
            foo =
                { "bar" ->
                   *[a] A
                }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun selector_term_attribute_reference() {
        val input = """
            foo =
                { -bar.baz ->
                   *[a] A
                }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun call_expression() {
        val input = """
            foo = { FOO() }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun call_expression_with_string_literal() {
        val input = """
            foo = { FOO("bar") }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun call_expression_with_number_literal() {
        val input = """
            foo = { FOO(1) }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun call_expression_with_message_reference() {
        val input = """
            foo = { FOO(bar) }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun call_expression_with_variable_reference() {
        val input = """
            foo = { FOO(${'$'}bar) }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun call_expression_with_named_number_literal() {
        val input = """
            foo = { FOO(bar: 1) }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun call_expression_with_named_string_literal() {
        val input = """
            foo = { FOO(bar: "bar") }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun call_expression_with_two_positional_arguments() {
        val input = """
            foo = { FOO(bar, baz) }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun call_expression_with_two_named_arguments() {
        val input = """
            foo = { FOO(bar: "bar", baz: "baz") }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun call_expression_with_positional_and_named_arguments() {
        val input = """
            foo = { FOO(bar, 1, baz: "baz") }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun term_reference_call() {
        val input = """
            foo = { -term() }
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

}

class SerializeEntryTest : SerializerTest() {

    @Test
    fun message() {
        val input = """
            # Attached comment
            key = Value
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }
}

class SerializeWhitespaceTest : SerializerTest() {

    @Test
    fun empty_lines() {
        val input = """
            key1 = Value 1
            
            
            key2 = Value 2
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun standalone_comment() {
        val input = """
            # Comment A
            
            foo = Foo
            
            # Comment B
            
            bar = Bar 2
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun group_comment() {
        val input = """
            ## Group A
            
            foo = Foo
            
            ## Group B
            
            bar = Bar 2
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }

    @Test
    fun resource_comment() {
        val input = """
            ### Resource Comment A
            
            foo = Foo
            
            ### Resource Comment B
            
            bar = Bar 2
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }
}
