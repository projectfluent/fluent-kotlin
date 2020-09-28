package org.projectfluent.syntax.visitor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.ast.BaseNode
import org.projectfluent.syntax.parser.FluentParser
import java.io.InvalidClassException

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

    @Test
    fun validatesPublicVisitMethodNames() {
        class InvalidVisitor : Visitor() {
            fun visitFoo(node: BaseNode) = run { }
        }

        assertThrows(InvalidClassException::class.java) {
            InvalidVisitor()
        }
    }

    @Test
    fun ignoresPrivateVisitMethodNames() {
        class ValidVisitor : Visitor() {
            private fun visitFoo(node: BaseNode) = run { }
        }

        ValidVisitor()
    }
}
