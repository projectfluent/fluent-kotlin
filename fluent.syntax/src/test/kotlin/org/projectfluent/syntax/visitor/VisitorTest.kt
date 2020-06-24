package org.projectfluent.syntax.visitor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.parser.FluentParser

class VisitorTest {
    val parser = FluentParser()

    @Test
    fun testVisitDelegation() {
        val visitor = TestableVisitor()
        val res = parser.parse(
            """
            |msg = foo {${'$'}var ->
            | *[other] bar
            } baz
        """.trimMargin()
        )

        visitor.visit(res)

        assertEquals(3, visitor.wordCount)
        assertEquals(2, visitor.patternCount)
        assertEquals(1, visitor.variantCount)
    }
}
