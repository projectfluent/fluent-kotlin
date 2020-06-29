package org.projectfluent.syntax.serializer

import org.projectfluent.syntax.ast.* // ktlint-disable no-wildcard-imports

private fun PatternElement.includesLine() = this is TextElement && value.contains("\n")

private fun PatternElement.isSelectExpr() = this is Placeable && expression is SelectExpression

private const val INDENTATION_SPACES = 4

private typealias SerializationResultWriter = (CharSequence) -> Unit

private fun SerializationResultWriter.write(vararg increments: CharSequence) = increments.forEach(this::invoke)

private fun SerializationResultWriter.indented(count: Int = INDENTATION_SPACES): SerializationResultWriter {
    val spaces = " ".repeat(count)
    return fun(increment: CharSequence) {
        this.write(increment.replace("\n".toRegex(), "\n$spaces"))
    }
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
    fun serialize(resource: Resource): CharSequence = toCharSequence(this::serialize, resource)

    fun serialize(resource: Resource, res: SerializationResultWriter): Unit =
        resource.body.forEach {
            when (it) {
                is Entry -> serializeEntry(it, res)
                is Whitespace -> res.write(it.content)
                is Junk -> it.content.takeIf { this.withJunk }?.let { junk -> res.write(junk) }
                else -> throw SerializeError("Unknown top-level entry type")
            }
        }

    /**
     * Serialize Message, Term, Whitespace, and Junk.
     */
    fun serialize(entry: TopLevel): CharSequence = toCharSequence(this::serialize, entry)

    fun serialize(entry: TopLevel, res: SerializationResultWriter): Unit =
        when (entry) {
            is Entry -> serializeEntry(entry, res)
            is Whitespace -> res.write(entry.content)
            is Junk -> entry.content.takeIf { this.withJunk }?.let { junk -> res.write(junk) } ?: Unit
            else -> throw SerializeError("Unknown top-level type: $entry")
        }

    /**
     * Serialize an Expression.
     *
     * This is useful to get a string representation of a simple Placeable.
     */
    fun serialize(expr: Expression): CharSequence = toCharSequence(this::serialize, expr)

    fun serialize(expr: Expression, res: SerializationResultWriter): Unit = serializeExpression(expr, res)

    /**
     * Serialize a VariantKey.
     *
     * Useful when displaying the options of a SelectExpression.
     */
    fun serialize(key: VariantKey): CharSequence = toCharSequence(this::serialize, key)

    fun serialize(key: VariantKey, res: SerializationResultWriter): Unit = serializeVariantKey(key, res)

    private fun serializeEntry(entry: Entry, res: SerializationResultWriter) =
        when (entry) {
            is Message -> serializeMessage(entry, res)
            is Term -> serializeTerm(entry, res)
            is Comment -> serializeComment(entry, res, "#")
            is GroupComment -> serializeComment(entry, res, "##")
            is ResourceComment -> serializeComment(entry, res, "###")
            else -> throw SerializeError("Unknown entry type: $entry")
        }

    private fun serializeComment(
        comment: BaseComment,
        res: SerializationResultWriter,
        prefix: CharSequence = "#"
    ) =
        comment.content
            .split("\n")
            .forEach {
                res.write(prefix)
                if (it.isNotEmpty()) {
                    res.write(" ", it)
                }
                res.write("\n")
            }

    private fun serializeMessage(message: Message, res: SerializationResultWriter) {
        message.comment?.let { serializeComment(it, res) }
        res.write(message.id.name, " =")
        message.value?.let { serializePattern(it, res) }
        message.attributes.forEach { serializeAttribute(it, res) }
        res.write("\n")
    }

    private fun serializeTerm(term: Term, res: SerializationResultWriter) {
        term.comment?.let { serializeComment(it, res) }
        res.write("-", term.id.name, " =")
        serializePattern(term.value, res)
        term.attributes.forEach { serializeAttribute(it, res) }
        res.write("\n")
    }

    private fun serializeAttribute(attribute: Attribute, res: SerializationResultWriter) {
        res.indented().write("\n", ".", attribute.id.name, " =")
        serializePattern(attribute.value, res.indented())
    }

    private fun serializePattern(pattern: Pattern, res: SerializationResultWriter) {
        val startOnLine = pattern.elements.any { it.isSelectExpr() || it.includesLine() }
        if (startOnLine) {
            res.indented().write("\n")
        } else {
            res.write(" ")
        }
        pattern.elements.forEach { serializeElement(it, res.indented()) }
    }

    private fun serializeElement(element: PatternElement, res: SerializationResultWriter) =
        when (element) {
            is TextElement -> res.write(element.value)
            is Placeable -> serializePlaceable(element, res)
            else -> throw SerializeError("Unknown element type: $element")
        }

    private fun serializePlaceable(placeable: Placeable, res: SerializationResultWriter): Unit =
        when (val expr = placeable.expression) {
            is Placeable -> {
                res.write("{")
                serializePlaceable(expr, res)
                res.write("}")
            }
            // Special-case select expression to control the whitespace around the
            // opening and the closing brace.
            is SelectExpression -> {
                res.write("{ ")
                serializeExpression(expr, res)
                res.write("}")
            }
            is Expression -> {
                res.write("{ ")
                serializeExpression(expr, res)
                res.write(" }")
            }
            else -> throw SerializeError("Unknown placeable type")
        }

    private fun serializeExpression(expr: Expression, res: SerializationResultWriter) {
        when (expr) {
            is StringLiteral -> res.write("\"", expr.value, "\"")
            is NumberLiteral -> res.write(expr.value)
            is VariableReference -> res.write("$", expr.id.name)
            is TermReference -> {
                res.write("-", expr.id.name)
                expr.attribute?.let { res.write(".", it.name) }
                expr.arguments?.let { serializeCallArguments(it, res) }
            }
            is MessageReference -> {
                res.write(expr.id.name)
                expr.attribute?.let { res.write(".", it.name) }
            }
            is FunctionReference -> {
                res.write(expr.id.name)
                serializeCallArguments(expr.arguments, res)
            }
            is SelectExpression -> {
                serializeExpression(expr.selector, res)
                res.write(" ->")
                expr.variants.forEach { serializeVariant(it, res) }
                res.write("\n")
            }
            else -> throw SerializeError("Unknown expression type: $expr")
        }
    }

    private fun serializeVariant(variant: Variant, res: SerializationResultWriter) {
        val marker = if (variant.default) {
            "*"
        } else {
            ""
        }
        res.indented(INDENTATION_SPACES - marker.length).write("\n")
        res.write(marker, "[")
        serializeVariantKey(variant.key, res)
        res.write("]")
        serializePattern(variant.value, res.indented())
    }

    private fun serializeCallArguments(expr: CallArguments, res: SerializationResultWriter) {
        val hasPositional = expr.positional.size > 0
        val hasNamed = expr.named.size > 0

        res.write("(")
        commaSeparated(this::serializeExpression, expr.positional, res)
        if (hasPositional && hasNamed) {
            res.write(", ")
        }
        commaSeparated(this::serializeNamedArgument, expr.named, res)
        res.write(")")
    }

    private fun serializeNamedArgument(arg: NamedArgument, res: SerializationResultWriter) {
        res.write(arg.name.name, ": ")
        serializeExpression(arg.value, res)
    }

    private fun serializeVariantKey(key: VariantKey, res: SerializationResultWriter) =
        when (key) {
            is Identifier -> res.write(key.name)
            is NumberLiteral -> res.write(key.value)
            else -> throw SerializeError("Unknown variant key type: $key")
        }

    private companion object {
        private fun <T> toCharSequence(serializer: (T, SerializationResultWriter) -> Unit, node: T): CharSequence =
            with(
                StringBuilder(),
                {
                    serializer.invoke(node) { this.append(it) }
                    this.toString()
                }
            )

        private fun <T> commaSeparated(
            serializer: (T, SerializationResultWriter) -> Unit,
            nodes: List<T>,
            res: SerializationResultWriter
        ) {
            nodes.forEachIndexed { i, it ->
                serializer.invoke(it, res)
                if (i != nodes.lastIndex) {
                    res.write(", ")
                }
            }
        }
    }
}
