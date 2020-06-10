package org.projectfluent.syntax.serializer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.ast.TopLevel
import org.projectfluent.syntax.parser.FluentParser

class SerializeEntryTest {
    private val parser = FluentParser()
    private val serializer = FluentSerializer()

    private fun parse(input: String): TopLevel {
        val resource = this.parser.parse(input)
        return resource.body[0]
    }

    private fun pretty(input: String): String {
        val first = this.parse(input)
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
