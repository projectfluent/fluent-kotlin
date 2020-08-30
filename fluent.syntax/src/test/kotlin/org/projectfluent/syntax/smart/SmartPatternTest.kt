package org.projectfluent.syntax.smart

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.ast.Pattern
import org.projectfluent.syntax.ast.Placeable
import org.projectfluent.syntax.ast.StringLiteral
import org.projectfluent.syntax.ast.TextElement

internal class SmartPatternTest {

    @Test
    fun antiElements() {
        val pattern = Pattern()
        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("Hi")
            )
        )
        var smarts = smartElements(pattern).toList()
        assertEquals(pattern.elements, smarts)
        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                Placeable(expression = StringLiteral("\\\\")),
                TextElement(" "),
                Placeable(expression = StringLiteral("""\""""))
            )
        )
        smarts = smartElements(pattern).toList()
        assertEquals(listOf(TextElement("""\ """")), smarts)
        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("Hi,"),
                Placeable(expression = StringLiteral("""\u0020""")),
                TextElement("there")
            )
        )
        smarts = smartElements(pattern).toList()
        assertEquals(listOf(TextElement("Hi, there")), smarts)
    }
}
