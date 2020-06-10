package org.projectfluent.syntax.serializer

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

