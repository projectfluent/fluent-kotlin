package org.projectfluent.syntax.serializer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.parser.FluentParser

class SerializeResourceTest {
    private val parser = FluentParser()
    private val serializer = FluentSerializer()

    fun pretty(ftl: String): String {
        val resource = this.parser.parse(ftl)
        val serialized = this.serializer.serialize(resource)
        return serialized.toString()
    }

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
    fun empty_lines() {
        val original = """
            key1 = Value 1
            
            
            key2 = Value 2
            
        """.trimIndent()
        val resource = this.parser.parse(original)
        val serialized = this.serializer.serialize(resource)
        assertEquals(original, serialized.toString())
    }
}

class SerializeEntryTest {
    private val parser = FluentParser()
    private val serializer = FluentSerializer()

    @Test
    fun message() {
        val original = """
            # Attached comment
            key = Value
            
        """.trimIndent()
        val entry = this.parser.parse(original).body[0]
        val serialized = this.serializer.serialize(entry)
        assertEquals(original, serialized.toString())
    }
}