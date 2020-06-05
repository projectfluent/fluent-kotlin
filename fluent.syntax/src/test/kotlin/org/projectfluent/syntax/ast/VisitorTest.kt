package org.projectfluent.syntax.ast

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.parser.FluentParser

class TestableVisitor : Visitor() {
    var patternCount = 0
    var variantCount = 0
    var wordCount = 0
    val WORDS = Regex("\\w+")
    fun visit_Pattern(node: Pattern) {
        super.generic_visit(node)
        patternCount++
    }
    fun visit_Variant(node: Variant) {
        super.generic_visit(node)
        variantCount++
    }
    fun visit_TextElement(node: TextElement) {
        wordCount += WORDS.findAll(node.value).count()
    }
}

internal class VisitorTest {
    val parser = FluentParser()
    @Test
    fun test_basics() {
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
