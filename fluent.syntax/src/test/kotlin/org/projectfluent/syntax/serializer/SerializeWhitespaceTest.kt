package org.projectfluent.syntax.serializer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.ast.Resource
import org.projectfluent.syntax.parser.FluentParser

class SerializeWhitespaceTest {
    private val parser = FluentParser()
    private val serializer = FluentSerializer()

    private fun parse(input: String): Resource {
        return this.parser.parse(input)
    }

    private fun pretty(input: String): String {
        val resource = this.parse(input)
        val serialized = this.serializer.serialize(resource)
        return serialized.toString()
    }

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
