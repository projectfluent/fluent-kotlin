package org.projectfluent.syntax.antipattern

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.ast.Pattern
import org.projectfluent.syntax.ast.Placeable
import org.projectfluent.syntax.ast.StringLiteral
import org.projectfluent.syntax.ast.TextElement

internal class AntiPatternTest {

    @Test
    fun antiElements() {
        val pattern = Pattern()
        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("Hi")
            )
        )
        var anti = antiElements(pattern).toList()
        assertEquals(pattern.elements, anti)
        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                Placeable(expression = StringLiteral("\\\\")),
                TextElement(" "),
                Placeable(expression = StringLiteral("""\""""))
            )
        )
        anti = antiElements(pattern).toList()
        assertEquals(listOf(TextElement("""\ """")), anti)
        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("Hi,"),
                Placeable(expression = StringLiteral("""\u0020""")),
                TextElement("there")
            )
        )
        anti = antiElements(pattern).toList()
        assertEquals(listOf(TextElement("Hi, there")), anti)
    }
}
