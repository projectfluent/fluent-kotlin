package org.projectfluent.syntax.serializer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.parser.FluentParser

class SerializerTest {
    val parser = FluentParser()
    val serializer = FluentSerializer()

    @Test
    fun message() {
        val original = """
            # Attached comment
            key = Value
            
        """.trimIndent()
        val resource = this.parser.parse(original)
        val serialized = this.serializer.serialize(resource)
        assertEquals(original, serialized.toString())
    }

    @Test
    fun comment_standalone() {
        val original = """
            # Standalone comment
            
            key = Value
            
        """.trimIndent()
        val resource = this.parser.parse(original)
        val serialized = this.serializer.serialize(resource)
        assertEquals(original, serialized.toString())
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
