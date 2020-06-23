package org.projectfluent.syntax.ast

import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties

/**
 * Base class for all Fluent AST nodes.
 *
 * All productions described in the ASDL subclass BaseNode, including Span and
 * Annotation.
 *
 */
abstract class BaseNode {
    fun nodeEquals(other: BaseNode?, ignoredFields: Array<String> = arrayOf("span")): Boolean {
        if (other == null) return false
        if (this::class != other::class) return false
        val otherMembers = hashMapOf<String, Any?>()
        other::class.memberProperties.forEach {
            if (it.visibility == KVisibility.PUBLIC && ! ignoredFields.contains(it.name)) {
                otherMembers[it.name] = it.getter.call(other)
            }
        }
        this::class.memberProperties.forEach {
            if (it.name in otherMembers) {
                val value = it.getter.call(this)
                val otherValue = otherMembers[it.name]
                if (value is Collection<*> && otherValue is Collection<*>) {
                    if (value.size != otherValue.size) return false
                    for ((left, right) in value.zip(otherValue)) {
                        if (! scalarsEqual(left!!, right!!, ignoredFields)) return false
                    }
                } else if (! scalarsEqual(value, otherValue, ignoredFields)) {
                    return false
                }
            }
        }
        return true
    }

    override fun equals(other: Any?): Boolean {
        if (other is BaseNode) return this.nodeEquals(other, arrayOf())
        return false
    }
}

private fun scalarsEqual(left: Any?, right: Any?, ignoredFields: Array<String>): Boolean {
    if (left is BaseNode && right is BaseNode) return left.nodeEquals(right, ignoredFields = ignoredFields)
    return left == right
}

/**
 * Base class for AST nodes which can have Spans.
 */
abstract class SyntaxNode(val span: Span? = null) : BaseNode()

/**
 * A Fluent file representation
 */
class Resource(vararg children: TopLevel) : SyntaxNode() {
    val body: List<TopLevel> = children.asList()
}

abstract class TopLevel : SyntaxNode()

/**
 * An abstract base class for useful elements of Resource.body.
 */
abstract class Entry : TopLevel()

data class Message(
        val id: Identifier,
        val value: Pattern?,
        val attributes: List<Attribute>,
        var comment: Comment? = null
) : Entry()

data class Term(
        val id: Identifier,
        val value: Pattern,
        val attributes: List<Attribute>,
        var comment: Comment? = null
) : Entry()

class Pattern(vararg elements: PatternElement) : SyntaxNode() {
    val elements: List<PatternElement> = elements.asList()
}

abstract class PatternElement(span: Span? = null) : SyntaxNode(span)

class TextElement(var value: String, span: Span? = null) : PatternElement(span)

interface InsidePlaceable

data class Placeable(val expression: InsidePlaceable) : InsidePlaceable, PatternElement()

abstract class Expression : CallArgument, InsidePlaceable, SyntaxNode()

abstract class Literal(val value: String) : Expression()

class StringLiteral(value: String) : Literal(value)

class NumberLiteral(value: String) : VariantKey, Literal(value)

data class MessageReference(val id: Identifier, val attribute: Identifier?) : Expression()

data class TermReference(val id: Identifier, val attribute: Identifier?, val arguments: CallArguments?) : Expression()

data class VariableReference(val id: Identifier) : Expression()

data class FunctionReference(val id: Identifier, val arguments: CallArguments) : Expression()

data class SelectExpression(val selector: Expression, val variants: List<Variant>) : Expression()

interface CallArgument

class CallArguments(
        val positional: List<Expression>,
        val named: List<NamedArgument>
) : SyntaxNode()

data class Attribute(val id: Identifier, val value: Pattern) : SyntaxNode()

interface VariantKey

data class Variant(val key: VariantKey, val value: Pattern, val default: Boolean) : SyntaxNode()

data class NamedArgument(val name: Identifier, val value: Literal) : CallArgument, SyntaxNode()

data class Identifier(val name: String) : VariantKey, SyntaxNode()

abstract class BaseComment(val content: String) : Entry()

class Comment(content: String) : BaseComment(content)

class GroupComment(content: String) : BaseComment(content)

class ResourceComment(content: String) : BaseComment(content)

data class Junk(val content: String, val annotations: List<Annotation>) : TopLevel()

/**
 * Represents top-level whitespace
 *
 * Extension of the data model in other implementations.
 */
data class Whitespace(val content: String) : TopLevel()

data class Span(val start: Int, val end: Int = start) : BaseNode()

class Annotation(val code: String, val message: String, val arguments: List<Any>, span: Span) : SyntaxNode(span)
