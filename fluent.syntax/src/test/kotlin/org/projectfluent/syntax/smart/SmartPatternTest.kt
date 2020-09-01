package org.projectfluent.syntax.smart

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.ast.*

internal class SmartPatternTest {

    @Test
    fun smartElements() {
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

        pattern.elements.clear()
        pattern.elements.addAll(
                arrayOf(
                        TextElement("Hi, "),
                        Placeable(expression = StringLiteral("""{""")),
                        TextElement(" there")
                )
        )
        smarts = smartElements(pattern).toList()
        assertEquals(listOf(TextElement("Hi, { there")), smarts)

        pattern.elements.clear()
        pattern.elements.addAll(
                arrayOf(
                        TextElement("Foo\n"),
                        Placeable(expression = StringLiteral(""".""")),
                        TextElement("bar")
                )
        )
        smarts = smartElements(pattern).toList()
        assertEquals(listOf(TextElement("Foo\n.bar")), smarts)

        pattern.elements.clear()
        pattern.elements.addAll(
                arrayOf(
                        TextElement("Foo "),
                        Placeable(
                                expression = SelectExpression(
                                        NumberLiteral("1"),
                                        mutableListOf(
                                                Variant(
                                                        Identifier("other"),
                                                        Pattern(
                                                                TextElement("bar "),
                                                                Placeable(
                                                                    StringLiteral("{-_-}")
                                                                )
                                                        ),
                                                        true
                                                )
                                        )
                                )
                        ),
                        TextElement(" baz")
                )
        )
        smarts = smartElements(pattern).toList()
        assertEquals(
                listOf(
                        TextElement("Foo "),
                        Placeable(
                                expression = SelectExpression(
                                        NumberLiteral("1"),
                                        mutableListOf(
                                                Variant(
                                                        Identifier("other"),
                                                        SmartPattern(TextElement("bar {-_-}")),
                                                        true
                                                )
                                        )
                                )
                        ),
                        TextElement(" baz")

                ),
                smarts
        )
    }
}
