package org.projectfluent.syntax.serializer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

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
