package org.projectfluent.syntax.visitor

import org.projectfluent.syntax.ast.Pattern
import org.projectfluent.syntax.ast.TextElement
import org.projectfluent.syntax.ast.Variant

class TestableVisitor : Visitor() {
    var patternCount = 0
    var variantCount = 0
    var wordCount = 0

    fun visitPattern(node: Pattern) {
        patternCount++
        genericVisit(node)
    }

    fun visitVariant(node: Variant) {
        variantCount++
        genericVisit(node)
    }

    fun visitTextElement(node: TextElement) {
        wordCount += WORDS.findAll(node.value).count()
    }

    private companion object {
        private val WORDS = Regex("\\w+")
    }
}
