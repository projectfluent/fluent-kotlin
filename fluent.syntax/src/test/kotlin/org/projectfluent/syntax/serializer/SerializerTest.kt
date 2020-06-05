package org.projectfluent.syntax.serializer

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.parser.FluentParser

class SerializerTest {
    val parser = FluentParser()
    val serializer = FluentSerializer()

    @Test
    fun foo() {
        val original = """
            msg = Foo
            
        """.trimIndent()
        val resource = this.parser.parse(original)
        val serialized = this.serializer.serialize(resource)
        assertEquals(original, serialized)
    }
}
