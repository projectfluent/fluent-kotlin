package org.projectfluent.syntax.processor

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.ast.*

internal class ProcessorTest {

    @Test
    fun toProcessedPattern() {
        val processor = Processor()

        val pattern = Pattern()
        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("Hi")
            )
        )
        assertEquals(pattern, processor.unescapeLiteralsToText(pattern))

        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                Placeable(expression = StringLiteral("\\\\")),
                TextElement(" "),
                Placeable(expression = StringLiteral("""\""""))
            )
        )
        assertEquals(
            Pattern(TextElement("""\ """")),
            processor.unescapeLiteralsToText(pattern)
        )

        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("Foo "),
                Placeable(expression = StringLiteral("Bar"))
            )
        )
        assertEquals(
            Pattern(TextElement("Foo Bar")),
            processor.unescapeLiteralsToText(pattern)
        )
        // The original Pattern isn't modified.
        assertEquals(
            Pattern(
                TextElement("Foo "),
                Placeable(expression = StringLiteral("Bar"))
            ),
            pattern
        )

        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("Hi,"),
                Placeable(expression = StringLiteral("""\u0020""")),
                TextElement("there")
            )
        )
        assertEquals(
            Pattern(TextElement("Hi, there")),
            processor.unescapeLiteralsToText(pattern)
        )

        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("Emoji: "),
                Placeable(expression = StringLiteral("""\U01f602"""))
            )
        )
        assertEquals(
            Pattern(TextElement("Emoji: \uD83D\uDE02")),
            processor.unescapeLiteralsToText(pattern)
        )
        assertEquals(
            Pattern(TextElement("Emoji: 😂")),
            processor.unescapeLiteralsToText(pattern)
        )

        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("Hi, "),
                Placeable(expression = StringLiteral("""{""")),
                TextElement(" there")
            )
        )
        assertEquals(
            Pattern(TextElement("Hi, { there")),
            processor.unescapeLiteralsToText(pattern)
        )

        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("Foo\n"),
                Placeable(expression = StringLiteral(""".""")),
                TextElement("bar")
            )
        )
        assertEquals(
            Pattern(TextElement("Foo\n.bar")),
            processor.unescapeLiteralsToText(pattern)
        )

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
        assertEquals(
            Pattern(
                TextElement("Foo "),
                Placeable(
                    expression = SelectExpression(
                        NumberLiteral("1"),
                        mutableListOf(
                            Variant(
                                Identifier("other"),
                                Pattern(TextElement("bar {-_-}")),
                                true
                            )
                        )
                    )
                ),
                TextElement(" baz")

            ),
            processor.unescapeLiteralsToText(pattern)
        )
    }

    @Test
    fun toRawPattern() {
        val processor = Processor()

        val pattern = Pattern()
        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("Hi")
            )
        )
        assertEquals(pattern, processor.escapeTextToLiterals(pattern))

        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("""\ """")
            )
        )
        assertEquals(pattern, processor.escapeTextToLiterals(pattern))

        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("Hi, {-_-}")
            )
        )
        assertEquals(
            Pattern(
                TextElement("Hi, "),
                Placeable(expression = StringLiteral("{")),
                TextElement("-_-"),
                Placeable(expression = StringLiteral("}"))
            ),
            processor.escapeTextToLiterals(pattern)
        )

        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("Foo\nbar")
            )
        )
        assertEquals(pattern, processor.escapeTextToLiterals(pattern))

        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("Foo\n*bar")
            )
        )
        assertEquals(
            Pattern(
                TextElement("Foo\n"),
                Placeable(expression = StringLiteral("*")),
                TextElement("bar")
            ),
            processor.escapeTextToLiterals(pattern)
        )

        pattern.elements.clear()
        pattern.elements.addAll(
            arrayOf(
                TextElement("\nFoo\nbar    ")
            )
        )
        assertEquals(
            Pattern(
                Placeable(expression = StringLiteral("")),
                TextElement("\nFoo\nbar    "),
                Placeable(expression = StringLiteral(""))
            ),
            processor.escapeTextToLiterals(pattern)
        )
    }
}
