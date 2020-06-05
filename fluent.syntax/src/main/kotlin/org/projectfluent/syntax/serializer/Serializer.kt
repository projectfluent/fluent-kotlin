package org.projectfluent.syntax.serializer

import org.projectfluent.syntax.ast.*

fun indent(content: String): String {
    return content.split("\n").joinToString("\n    ")
}

fun includesLine(elem: PatternElement): Boolean {
    return elem is TextElement && elem.value.contains("\n")
}

fun isSelectExpr(elem: PatternElement): Boolean {
    return elem is Placeable
            && elem.expression is SelectExpression
}

class FluentSerializer(var withJunk: Boolean = false) {
    fun serialize(resource: Resource): String {
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

        return builder.toString()
    }

    fun serializeEntry(entry: Entry): String {
        when (entry) {
            is Message -> return serializeMessage(entry)
            is Term -> return serializeTerm(entry)
            is Comment -> return serializeComment(entry, "#")
            is GroupComment -> return serializeComment(entry, "##")
            is ResourceComment -> return serializeComment(entry, "###")
        }
        throw SerializeError("Unknown entry type: $entry")
    }
}


fun serializeComment(comment: BaseComment, prefix: String = "#"): String {
    val builder = StringBuilder()
    val lines = comment.content.split("\n")
    for (line in lines) {
        if (line.isNotEmpty()) {
            builder.append("$prefix $line", "\n")
        } else {
            builder.append(prefix, "\n")
        }
    }
    return builder.toString()
}


fun serializeMessage(message: Message): String {
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
    return builder.toString()
}


fun serializeTerm(term: Term): String {
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
    return builder.toString()
}


fun serializeAttribute(attribute: Attribute): String {
    val value = indent(serializePattern(attribute.value))
    return "\n    .${attribute.id.name} =$value"
}


fun serializePattern(pattern: Pattern): String {
    val content = pattern.elements.map(::serializeElement).joinToString("")
    val startOnLine =
            pattern.elements.any(::isSelectExpr) ||
                    pattern.elements.any(::includesLine)

    if (startOnLine) {
        return "\n    ${indent(content)}"
    }

    return " $content"
}


fun serializeElement(element: PatternElement): String {
    when (element) {
        is TextElement -> return element.value
        is Placeable -> return serializePlaceable(element)
    }
    throw SerializeError("Unknown element type: $element")
}


fun serializePlaceable(placeable: Placeable): String {
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


fun serializeExpression(expr: Expression): String {
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
            return builder.toString()
        }
        is MessageReference -> {
            val builder = StringBuilder()
            builder.append(expr.id.name)
            expr.attribute?.let {
                builder.append(".", it.name)
            }
            return builder.toString()
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
            return builder.toString()
        }
    }
    throw SerializeError("Unknown expression type: $expr")
}


fun serializeVariant(variant: Variant): String {
    val key = serializeVariantKey(variant.key)
    val value = indent(serializePattern(variant.value))

    if (variant.default) {
        return "\n   *[$key]$value"
    }

    return "\n    [$key]$value"
}


fun serializeCallArguments(expr: CallArguments): String {
    val positional = expr.positional.map(::serializeExpression).joinToString(", ")
    val named = expr.named.map(::serializeNamedArgument).joinToString(", ")
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


fun serializeNamedArgument(arg: NamedArgument): String {
    val value = serializeExpression(arg.value)
    return "${arg.name.name}: $value"
}

fun serializeVariantKey(key: VariantKey): String {
    when (key) {
        is Identifier -> return key.name
        is NumberLiteral -> return key.value
    }
    throw SerializeError("Unknown variant key type: $key")
}

