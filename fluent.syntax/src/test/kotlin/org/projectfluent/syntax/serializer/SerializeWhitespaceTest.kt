package org.projectfluent.syntax.serializer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
