package org.projectfluent.syntax.serializer

import org.projectfluent.syntax.ast.* // ktlint-disable no-wildcard-imports

private fun indentExceptFirstLine(content: CharSequence) =
    content.split("\n").joinToString("\n    ")

private fun PatternElement.includesLine() =
    this is TextElement && value.contains("\n")

private fun PatternElement.isSelectExpr() =
    this is Placeable && expression is SelectExpression

private fun Pattern.shouldStartOnNewLine(): Boolean {
    val isMultiline = this.elements.any { it.isSelectExpr() || it.includesLine() }
    if (isMultiline) {
        val firstElement = this.elements.elementAtOrNull(0)
        if (firstElement is TextElement) {
            val firstChar = firstElement.value.elementAtOrNull(0)
            // Due to the indentation requirement the following characters may not appear
            // as the first character on a new line.
            if (firstChar == '[' || firstChar == '.' || firstChar == '*') {
                return false
            }
        }

        return true
    }

    return false
}

/**
 * Serialize Fluent nodes to `CharSequence`.
 *
 * @property withJunk serialize Junk entries or not.
 */
class FluentSerializer(private val withJunk: Boolean = false) {
    /**
     * Serialize a Resource.
     */
    fun serialize(resource: Resource): CharSequence =
        resource.body
            .mapNotNull {
                when (it) {
                    is Entry -> serializeEntry(it)
                    is Whitespace -> it.content
                    is Junk -> it.content.takeIf { this.withJunk }
                    else -> throw SerializeError("Unknown top-level entry type")
                }
            }
            .joinToString("")

    /**
     * Serialize Message, Term, Whitespace, and Junk.
     */
    fun serialize(entry: TopLevel): CharSequence =
        when (entry) {
            is Entry -> serializeEntry(entry)
            is Whitespace -> entry.content
            is Junk -> entry.content
            else -> throw SerializeError("Unknown top-level type: $entry")
        }

    /**
     * Serialize an Expression.
     *
     * This is useful to get a string representation of a simple Placeable.
     */
    fun serialize(expr: Expression): CharSequence = serializeExpression(expr)

    /**
     * Serialize a VariantKey.
     *
     * Useful when displaying the options of a SelectExpression.
     */
    fun serialize(key: VariantKey): CharSequence = serializeVariantKey(key)

    private fun serializeEntry(entry: Entry) =
        when (entry) {
            is Message -> serializeMessage(entry)
            is Term -> serializeTerm(entry)
            is Comment -> serializeComment(entry, "#")
            is GroupComment -> serializeComment(entry, "##")
            is ResourceComment -> serializeComment(entry, "###")
            else -> throw SerializeError("Unknown entry type: $entry")
        }

    private fun serializeComment(comment: BaseComment, prefix: CharSequence = "#") =
        comment.content.split("\n")
            .joinToString(
                "",
                transform = {
                    if (it.isNotEmpty()) {
                        "$prefix $it\n"
                    } else {
                        "$prefix\n"
                    }
                }
            )

    private fun serializeMessage(message: Message): CharSequence {
        val builder = StringBuilder()

        message.comment?.let {
            builder.append(serializeComment(it))
        }

        builder.append(message.id.name, " =")

        message.value?.let {
            builder.append(serializePattern(it))
        }

        for (attribute in message.attributes) {
            builder.append(serializeAttribute(attribute))
        }

        builder.append("\n")
        return builder
    }

    private fun serializeTerm(term: Term): CharSequence {
        val builder = StringBuilder()

        term.comment?.let {
            builder.append(serializeComment(it))
        }

        builder.append("-" + term.id.name + " =")
        builder.append(serializePattern(term.value))

        for (attribute in term.attributes) {
            builder.append(serializeAttribute(attribute))
        }

        builder.append("\n")
        return builder
    }

    private fun serializeAttribute(attribute: Attribute): CharSequence {
        val value = indentExceptFirstLine(serializePattern(attribute.value))
        return "\n    .${attribute.id.name} =$value"
    }

    private fun serializePattern(pattern: Pattern): CharSequence {
        val elements = pattern.elements.map(::serializeElement)
        val content = indentExceptFirstLine(elements.joinToString(""))

        return if (pattern.shouldStartOnNewLine()) {
            "\n    $content"
        } else {
            " $content"
        }
    }

    private fun serializeElement(element: PatternElement) =
        when (element) {
            is TextElement -> element.value
            is Placeable -> serializePlaceable(element)
            else -> throw SerializeError("Unknown element type: $element")
        }

    private fun serializePlaceable(placeable: Placeable): CharSequence =
        when (val expr = placeable.expression) {
            is Placeable -> "{${serializePlaceable(expr)}}"
            // Special-case select expression to control the whitespace around the
            // opening and the closing brace.
            is SelectExpression -> "{ ${serializeExpression(expr)}}"
            is Expression -> "{ ${serializeExpression(expr)} }"
            else -> throw SerializeError("Unknown placeable type")
        }

    private fun serializeExpression(expr: Expression): CharSequence {
        return when (expr) {
            is StringLiteral -> "\"${expr.value}\""
            is NumberLiteral -> expr.value
            is VariableReference -> "$${expr.id.name}"
            is TermReference -> {
                val builder = StringBuilder()
                builder.append("-", expr.id.name)
                expr.attribute?.let {
                    builder.append(".", it.name)
                }
                expr.arguments?.let {
                    builder.append(serializeCallArguments(it))
                }
                builder
            }
            is MessageReference -> {
                val builder = StringBuilder()
                builder.append(expr.id.name)
                expr.attribute?.let {
                    builder.append(".", it.name)
                }
                builder
            }
            is FunctionReference -> "${expr.id.name}${serializeCallArguments(expr.arguments)}"
            is SelectExpression -> {
                val builder = StringBuilder()
                builder.append(serializeExpression(expr.selector), " ->")
                expr.variants.forEach { builder.append(serializeVariant(it)) }
                builder.append("\n")
            }
            else -> throw SerializeError("Unknown expression type: $expr")
        }
    }

    private fun serializeVariant(variant: Variant): CharSequence {
        val key = serializeVariantKey(variant.key)
        val value = indentExceptFirstLine(serializePattern(variant.value))

        return if (variant.default) {
            "\n   *[$key]$value"
        } else {
            "\n    [$key]$value"
        }
    }

    private fun serializeCallArguments(expr: CallArguments): CharSequence {
        val positional = expr.positional.joinToString(", ", transform = ::serializeExpression)
        val named = expr.named.joinToString(", ", transform = ::serializeNamedArgument)
        val hasPositional = expr.positional.size > 0
        val hasNamed = expr.named.size > 0

        return if (hasPositional && hasNamed) {
            "($positional, $named)"
        } else if (hasPositional) {
            "($positional)"
        } else if (hasNamed) {
            "($named)"
        } else {
            "()"
        }
    }

    private fun serializeNamedArgument(arg: NamedArgument) = "${arg.name.name}: ${serializeExpression(arg.value)}"

    private fun serializeVariantKey(key: VariantKey) =
        when (key) {
            is Identifier -> key.name
            is NumberLiteral -> key.value
            else -> throw SerializeError("Unknown variant key type: $key")
        }
}
