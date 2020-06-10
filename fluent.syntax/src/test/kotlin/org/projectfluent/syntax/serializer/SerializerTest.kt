package org.projectfluent.syntax.serializer

import org.projectfluent.syntax.parser.FluentParser

abstract class SerializerTest {
    protected val parser = FluentParser()
    protected val serializer = FluentSerializer()

    open fun pretty(input: String): String {
        val resource = this.parser.parse(input)
        val serialized = this.serializer.serialize(resource)
        return serialized.toString()
    }
}

