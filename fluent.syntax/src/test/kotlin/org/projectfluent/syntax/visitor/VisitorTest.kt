package org.projectfluent.syntax.visitor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.ast.Pattern
import org.projectfluent.syntax.ast.TextElement
import org.projectfluent.syntax.ast.Variant
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

private class TestableVisitor : Visitor() {
    var patternCount = 0
    var variantCount = 0
    var wordCount = 0

    fun visitPattern(node: Pattern) {
        patternCount++
        visitProperties(node)
    }

    fun visitVariant(node: Variant) {
        variantCount++
        visitProperties(node)
    }

    fun visitTextElement(node: TextElement) {
        wordCount += WORDS.findAll(node.value).count()
    }

    private companion object {
        private val WORDS = Regex("\\w+")
    }
}
