package org.projectfluent.syntax.ast

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.parser.FluentParser

class TestableVisitor : Visitor() {
    var patternCount = 0
    var variantCount = 0
    var wordCount = 0
    val WORDS = Regex("\\w+")
    fun visitPattern(node: Pattern) {
        super.genericVisit(node)
        patternCount++
    }
    fun visitVariant(node: Variant) {
        super.genericVisit(node)
        variantCount++
    }
    fun visitTextElement(node: TextElement) {
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

internal class ChildrenOfTest {
    @Test
    fun test_childrenOf() {
        val variant = Variant(Identifier("other"), Pattern(), true)
        val variant_props = childrenOf(variant)
        assertEquals(
            listOf("default", "key", "span", "value"),
            variant_props.map { (name, _) -> name }.sorted().toList()
        )
    }
}
