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
abstract class SyntaxNode : BaseNode() {
    var span: Span? = null
    fun addSpan(start: Int, end: Int) {
        this.span = Span(start, end)
    }
}

/**
 * A Fluent file representation
 */
class Resource(vararg children: TopLevel) : SyntaxNode() {
    val body: MutableList<TopLevel> = mutableListOf()

    init {
        this.body += children
    }
}

abstract class TopLevel : SyntaxNode()

/**
 * An abstract base class for useful elements of Resource.body.
 */
abstract class Entry : TopLevel()

data class Message(var id: Identifier, var value: Pattern?) : Entry() {
    var attributes: MutableList<Attribute> = mutableListOf()
    var comment: Comment? = null
}

data class Term(var id: Identifier, var value: Pattern) : Entry() {
    var attributes: MutableList<Attribute> = mutableListOf()
    var comment: Comment? = null
}

class Pattern(vararg elements: PatternElement) : SyntaxNode() {
    val elements: MutableList<PatternElement> = mutableListOf()

    init {
        this.elements += elements
    }
}

abstract class PatternElement : SyntaxNode()

data class TextElement(var value: String) : PatternElement()

interface InsidePlaceable

data class Placeable(var expression: InsidePlaceable) : InsidePlaceable, PatternElement()

abstract class Expression : CallArgument, InsidePlaceable, SyntaxNode()

abstract class Literal(val value: String) : Expression()

class StringLiteral(value: String) : Literal(value)

class NumberLiteral(value: String) : VariantKey, Literal(value)

data class MessageReference(var id: Identifier, var attribute: Identifier? = null) : Expression()

data class TermReference(var id: Identifier, var attribute: Identifier? = null, var arguments: CallArguments? = null) : Expression()

data class VariableReference(var id: Identifier) : Expression()

data class FunctionReference(var id: Identifier, var arguments: CallArguments) : Expression()

data class SelectExpression(var selector: Expression, var variants: MutableList<Variant>) : Expression()

interface CallArgument

class CallArguments : SyntaxNode() {
    val positional: MutableList<Expression> = mutableListOf()
    val named: MutableList<NamedArgument> = mutableListOf()
}

data class Attribute(var id: Identifier, var value: Pattern) : SyntaxNode()

interface VariantKey

data class Variant(var key: VariantKey, var value: Pattern, var default: Boolean) : SyntaxNode()

data class NamedArgument(var name: Identifier, var value: Literal) : CallArgument, SyntaxNode()

data class Identifier(var name: String) : VariantKey, SyntaxNode()

abstract class BaseComment(var content: String) : Entry()

class Comment(content: String) : BaseComment(content)

class GroupComment(content: String) : BaseComment(content)

class ResourceComment(content: String) : BaseComment(content)

data class Junk(val content: String) : TopLevel() {
    val annotations: MutableList<Annotation> = mutableListOf()
    fun addAnnotation(annotation: Annotation) {
        this.annotations.add(annotation)
    }
}

/**
 * Represents top-level whitespace
 *
 * Extension of the data model in other implementations.
 */
data class Whitespace(val content: String) : TopLevel()

data class Span(var start: Int, var end: Int) : BaseNode()

data class Annotation(var code: String, var message: String) : SyntaxNode() {
    val arguments: MutableList<Any> = mutableListOf()
}
