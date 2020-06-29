package org.projectfluent.syntax.serializer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectfluent.syntax.ast.Resource
import org.projectfluent.syntax.parser.FluentParser
import java.io.ByteArrayOutputStream

class FluentParserAndSerializerTest {

    private val parser = FluentParser()
    private val serializer = FluentSerializer()

    private fun parseAndSerialize(input: String): String {
        val resource = parser.parse(input)
        val serialized = serializer.serialize(resource)
        return serialized.toString()
    }

    @Test
    fun simpleMessageWithoutEol() {
        val input = "foo = Foo"
        assertEquals("foo = Foo\n", parseAndSerialize(input))
    }

    @Test
    fun simpleMessage() {
        val input =
            """
            foo = Foo
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun simpleTerm() {
        val input =
            """
            -foo = Foo
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun twoSimpleMessages() {
        val input =
            """
            foo = Foo
            bar = Bar
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun blockMultilineMessage() {
        val input =
            """
            foo =
                Foo
                Bar
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun inlineMultilineMessage() {
        val input =
            """
            foo = Foo
                Bar
            
            """.trimIndent()
        val expected =
            """
            foo =
                Foo
                Bar
            
            """.trimIndent()
        assertEquals(expected, parseAndSerialize(input))
    }

    @Test
    fun messageReference() {
        val input =
            """
            foo = Foo { bar }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun messageAttributeReference() {
        val input =
            """
            foo = Foo { bar.baz }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun termReference() {
        val input =
            """
            foo = Foo { -bar }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun variableReference() {
        val input =
            """
            foo = Foo { ${'$'}bar }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun numberLiteral() {
        val input =
            """
            foo = Foo { 1 }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun stringLiteral() {
        val input =
            """
            foo = Foo { "bar" }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun resourceComment() {
        val input =
            """
            ### A multiline
            ### resource comment.
            
            foo = Foo
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun groupComment() {
        val input =
            """
            foo = Foo
            
            ## Comment Header
            ##
            ## A multiline
            ## group comment
            
            bar = Bar
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun messageComment() {
        val input =
            """
            # A multiline
            # message comment.
            foo = Foo
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun standaloneComment() {
        val input =
            """
            foo = Foo
            
            # A standalone comment
            
            bar = Bar
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun multilineWithPlaceable() {
        val input =
            """
            foo =
                Foo { bar }
                Baz
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun attribute() {
        val input =
            """
            foo =
                .attr = Foo Attr
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun multilineAttribute() {
        val input =
            """
            foo =
                .attr =
                    Foo Attr
                    Continued
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun twoAttribute() {
        val input =
            """
            foo =
                .attr-a = Foo Attr A
                .attr-b = Foo Attr B
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun valueAndAttributes() {
        val input =
            """
            foo = Foo Value
                .attr-a = Foo Attr A
                .attr-b = Foo Attr B
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun multilineValueAndAttributes() {
        val input =
            """
            foo =
                Foo Value
                Continued
                .attr-a = Foo Attr A
                .attr-b = Foo Attr B
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun selectExpression() {
        val input =
            """
            foo =
                { ${'$'}sel ->
                   *[a] A
                    [b] B
                }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun multilineVariant() {
        val input =
            """
            foo =
                { ${'$'}sel ->
                   *[a]
                        AAA
                        BBB
                }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun multilineVariantWithFirstLineInline() {
        val input =
            """
            foo =
                { ${'$'}sel ->
                   *[a] AAA
                        BBB
                }
            
            """.trimIndent()
        val expected =
            """
            foo =
                { ${'$'}sel ->
                   *[a]
                        AAA
                        BBB
                }
            
            """.trimIndent()
        assertEquals(expected, parseAndSerialize(input))
    }

    @Test
    fun variantKeyNumber() {
        val input =
            """
            foo =
                { ${'$'}sel ->
                   *[1] 1
                }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun selectExpressionInBlockPattern() {
        val input =
            """
            foo =
                Foo { ${'$'}sel ->
                   *[a] A
                    [b] B
                }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun selectExpressionInInlinePattern() {
        val input =
            """
            foo = Foo { ${'$'}sel ->
                   *[a] A
                    [b] B
                }
            
            """.trimIndent()
        val expected =
            """
            foo =
                Foo { ${'$'}sel ->
                   *[a] A
                    [b] B
                }
            
            """.trimIndent()
        assertEquals(expected, parseAndSerialize(input))
    }

    @Test
    fun selectExpressionInMultilinePattern() {
        val input =
            """
            foo =
                Foo
                Bar { ${'$'}sel ->
                   *[a] A
                    [b] B
                }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun nestedSelectExpression() {
        val input =
            """
            foo =
                { ${'$'}a ->
                   *[a]
                        { ${'$'}b ->
                           *[b] Foo
                        }
                }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun selectorVariableReference() {
        val input =
            """
            foo =
                { ${'$'}bar ->
                   *[a] A
                }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun selectorNumberLiteral() {
        val input =
            """
            foo =
                { 1 ->
                   *[a] A
                }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun selectorStringLiteral() {
        val input =
            """
            foo =
                { "bar" ->
                   *[a] A
                }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun selectorTermAttributeReference() {
        val input =
            """
            foo =
                { -bar.baz ->
                   *[a] A
                }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun callExpression() {
        val input =
            """
            foo = { FOO() }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun callExpressionWithStringLiteral() {
        val input =
            """
            foo = { FOO("bar") }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun callExpressionWithNumberLiteral() {
        val input =
            """
            foo = { FOO(1) }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun callExpressionWithMessageReference() {
        val input =
            """
            foo = { FOO(bar) }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun callExpressionWithVariableReference() {
        val input =
            """
            foo = { FOO(${'$'}bar) }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun callExpressionWithNamedNumberLiteral() {
        val input =
            """
            foo = { FOO(bar: 1) }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun callExpressionWithNamedStringLiteral() {
        val input =
            """
            foo = { FOO(bar: "bar") }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun callExpressionWithTwoPositionalArguments() {
        val input =
            """
            foo = { FOO(bar, baz) }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun callExpressionWithTwoNamedArguments() {
        val input =
            """
            foo = { FOO(bar: "bar", baz: "baz") }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun callExpressionWithPositionalAndNamedArguments() {
        val input =
            """
            foo = { FOO(bar, 1, baz: "baz") }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun termReferenceCall() {
        val input =
            """
            foo = { -term() }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun nestedPlaceable() {
        val input =
            """
            foo = {{ FOO() }}
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun backslashInTextElement() {
        val input =
            """
            foo = \{ placeable }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun escapedSpecialCharInStringLiteral() {
        val input =
            """
            foo = { "Escaped \" quote" }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun unicodeEscapeSequence() {
        val input =
            """
            foo = { "\u0065" }
            
            """.trimIndent()
        assertEquals(input, parseAndSerialize(input))
    }

    @Test
    fun writesToProvidedStringBuilder() {
        val input = "foo = Foo"
        val resource: Resource = parser.parse(input)

        val expected = serializer.serialize(resource)
        val actual = with(
            StringBuilder(),
            {
                serializer.serialize(resource) { this.append(it) }
                this.toString()
            }
        )

        assertEquals(expected, actual)
    }

    @Test
    fun writesToProvidedStream() {
        val input = "foo = Foo"
        val resource: Resource = parser.parse(input)

        val expected = serializer.serialize(resource)
        val actual = with(
            ByteArrayOutputStream(),
            {
                serializer.serialize(resource) { this.writeBytes(it.toString().toByteArray()) }
                this.toString()
            }
        )

        assertEquals(expected, actual)
    }
}
