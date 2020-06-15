package org.projectfluent.syntax.serializer

import org.projectfluent.syntax.ast.*

fun indent(content: CharSequence): String {
    return content.split("\n").joinToString("\n    ")
}

fun includesLine(elem: PatternElement): Boolean {
    return elem is TextElement && elem.value.contains("\n")
}

fun isSelectExpr(elem: PatternElement): Boolean {
    return elem is Placeable &&
        elem.expression is SelectExpression
}

class FluentSerializer(var withJunk: Boolean = false) {
    fun serialize(resource: Resource): CharSequence {
        val builder = StringBuilder()

        entries@ for (entry in resource.body) {
            val serialized = when (entry) {
                is Entry -> serializeEntry(entry)
                is Whitespace -> entry.content
                is Junk -> {
                    if (this.withJunk) {
                        entry.content
                    } else {
                        continue@entries
                    }
                }
                else -> throw SerializeError("Unknown top-level entry type")
            }
            builder.append(serialized)
        }

        return builder
    }

    fun serialize(entry: TopLevel): CharSequence {
        when (entry) {
            is Entry -> return serializeEntry(entry)
            is Whitespace -> return entry.content
            is Junk -> return entry.content
        }
        throw SerializeError("Unknown top-level type: $entry")
    }

    fun serialize(expr: Expression): CharSequence {
        return serializeExpression(expr)
    }

    fun serialize(key: VariantKey): CharSequence {
        return serializeVariantKey(key)
    }

    private fun serializeEntry(entry: Entry): CharSequence {
        when (entry) {
            is Message -> return serializeMessage(entry)
            is Term -> return serializeTerm(entry)
            is Comment -> return serializeComment(entry, "#")
            is GroupComment -> return serializeComment(entry, "##")
            is ResourceComment -> return serializeComment(entry, "###")
        }
        throw SerializeError("Unknown entry type: $entry")
    }

    private fun serializeComment(comment: BaseComment, prefix: String = "#"): CharSequence {
        val builder = StringBuilder()
        val lines = comment.content.split("\n")
        for (line in lines) {
            if (line.isNotEmpty()) {
                builder.append("$prefix $line", "\n")
            } else {
                builder.append(prefix, "\n")
            }
        }
        return builder
    }

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
        val value = indent(serializePattern(attribute.value))
        return "\n    .${attribute.id.name} =$value"
    }

    private fun serializePattern(pattern: Pattern): CharSequence {
        val startOnLine =
            pattern.elements.any(::isSelectExpr) ||
                pattern.elements.any(::includesLine)
        val elements = pattern.elements.map(::serializeElement)
        val content = indent(elements.joinToString(""))

        if (startOnLine) {
            return "\n    $content"
        }

        return " $content"
    }

    private fun serializeElement(element: PatternElement): CharSequence {
        when (element) {
            is TextElement -> return element.value
            is Placeable -> return serializePlaceable(element)
        }
        throw SerializeError("Unknown element type: $element")
    }

    private fun serializePlaceable(placeable: Placeable): CharSequence {
        val expr = placeable.expression
        when (expr) {
            is Placeable -> return "{${serializePlaceable(expr)}}"
            // Special-case select expression to control the whitespace around the
            // opening and the closing brace.
            is SelectExpression -> return "{ ${serializeExpression(expr)}}"
            is Expression -> return "{ ${serializeExpression(expr)} }"
        }
        throw SerializeError("Unknown placeable type")
    }

    private fun serializeExpression(expr: Expression): CharSequence {
        when (expr) {
            is StringLiteral -> return "\"${expr.value}\""
            is NumberLiteral -> return expr.value
            is VariableReference -> return "$${expr.id.name}"
            is TermReference -> {
                val builder = StringBuilder()
                builder.append("-", expr.id.name)
                expr.attribute?.let {
                    builder.append(".", it.name)
                }
                expr.arguments?.let {
                    builder.append(serializeCallArguments(it))
                }
                return builder
            }
            is MessageReference -> {
                val builder = StringBuilder()
                builder.append(expr.id.name)
                expr.attribute?.let {
                    builder.append(".", it.name)
                }
                return builder
            }
            is FunctionReference ->
                return "${expr.id.name}${serializeCallArguments(expr.arguments)}"
            is SelectExpression -> {
                val builder = StringBuilder()
                val selector = serializeExpression(expr.selector)
                builder.append(selector, " ->")
                for (variant in expr.variants) {
                    builder.append(serializeVariant(variant))
                }
                builder.append("\n")
                return builder
            }
        }
        throw SerializeError("Unknown expression type: $expr")
    }

    private fun serializeVariant(variant: Variant): CharSequence {
        val key = serializeVariantKey(variant.key)
        val value = indent(serializePattern(variant.value))

        if (variant.default) {
            return "\n   *[$key]$value"
        }

        return "\n    [$key]$value"
    }

    private fun serializeCallArguments(expr: CallArguments): CharSequence {
        val positional = expr.positional.joinToString(", ", transform = ::serializeExpression)
        val named = expr.named.joinToString(", ", transform = ::serializeNamedArgument)
        if (expr.positional.size > 0 && expr.named.size > 0) {
            return "($positional, $named)"
        }
        if (expr.positional.size > 0) {
            return "($positional)"
        }
        if (expr.named.size > 0) {
            return "($named)"
        }
        return "()"
    }

    private fun serializeNamedArgument(arg: NamedArgument): CharSequence {
        val value = serializeExpression(arg.value)
        return "${arg.name.name}: $value"
    }

    private fun serializeVariantKey(key: VariantKey): CharSequence {
        when (key) {
            is Identifier -> return key.name
            is NumberLiteral -> return key.value
        }
        throw SerializeError("Unknown variant key type: $key")
    }
}
