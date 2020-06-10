package org.projectfluent.syntax.serializer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SerializeEntryTest : SerializerTest() {

    override fun pretty(input: String): String {
        val resource = this.parser.parse(input)
        val first = resource.body[0]
        val serialized = this.serializer.serialize(first)
        return serialized.toString()
    }

    @Test
    fun message() {
        val input = """
            # Attached comment
            key = Value
            
        """.trimIndent()
        assertEquals(input, this.pretty(input))
    }
}
