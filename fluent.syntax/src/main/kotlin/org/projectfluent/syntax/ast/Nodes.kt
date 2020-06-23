package org.projectfluent.syntax.ast

import kotlin.reflect.KClass
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

    override fun equals(other: Any?) =
            if (other is BaseNode) {
                this.equals(other, emptySet())
            } else {
                false
            }

    fun equals(other: BaseNode, ignoredFields: Set<String> = setOf("span")): Boolean =
            if (this::class == other::class) {
                publicMemberProperties(this::class, ignoredFields).all {
                    val thisValue = it.getter.call(this)
                    val otherValue = it.getter.call(other)
                    if (thisValue is Collection<*> && otherValue is Collection<*>) {
                        if (thisValue.size == otherValue.size) {
                            thisValue.zip(otherValue).all { (a, b) -> scalarsEqual(a, b, ignoredFields) }
                        } else {
                            false
                        }
                    } else {
                        scalarsEqual(thisValue, otherValue, ignoredFields)
                    }
                }
            } else {
                false
            }

    private companion object {
        private fun publicMemberProperties(clazz: KClass<*>, ignoredFields: Set<String>) =
                clazz.memberProperties
                        .filter { it.visibility == KVisibility.PUBLIC }
                        .filterNot { ignoredFields.contains(it.name) }

        private fun scalarsEqual(left: Any?, right: Any?, ignoredFields: Set<String>) =
                if (left is BaseNode && right is BaseNode) {
                    left.equals(right, ignoredFields)
                } else {
                    left == right
                }
    }
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

class Message(var id: Identifier, var value: Pattern?) : Entry() {
    var attributes: MutableList<Attribute> = mutableListOf()
    var comment: Comment? = null
}

class Term(var id: Identifier, var value: Pattern) : Entry() {
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

class TextElement(var value: String) : PatternElement()

interface InsidePlaceable

class Placeable(var expression: InsidePlaceable) : InsidePlaceable, PatternElement()

abstract class Expression : CallArgument, InsidePlaceable, SyntaxNode()

abstract class Literal(val value: String) : Expression()

class StringLiteral(value: String) : Literal(value)

class NumberLiteral(value: String) : VariantKey, Literal(value)

class MessageReference(var id: Identifier, var attribute: Identifier? = null) : Expression()

class TermReference(var id: Identifier, var attribute: Identifier? = null, var arguments: CallArguments? = null) : Expression()

class VariableReference(var id: Identifier) : Expression()

class FunctionReference(var id: Identifier, var arguments: CallArguments) : Expression()

class SelectExpression(var selector: Expression, var variants: MutableList<Variant>) : Expression()

interface CallArgument

class CallArguments : SyntaxNode() {
    val positional: MutableList<Expression> = mutableListOf()
    val named: MutableList<NamedArgument> = mutableListOf()
}

class Attribute(var id: Identifier, var value: Pattern) : SyntaxNode()

interface VariantKey

class Variant(var key: VariantKey, var value: Pattern, var default: Boolean) : SyntaxNode()

class NamedArgument(var name: Identifier, var value: Literal) : CallArgument, SyntaxNode()

class Identifier(var name: String) : VariantKey, SyntaxNode()

abstract class BaseComment(var content: String) : Entry()

class Comment(content: String) : BaseComment(content)

class GroupComment(content: String) : BaseComment(content)

class ResourceComment(content: String) : BaseComment(content)

class Junk(val content: String) : TopLevel() {
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
class Whitespace(val content: String) : TopLevel()

class Span(var start: Int, var end: Int) : BaseNode()

class Annotation(var code: String, var message: String) : SyntaxNode() {
    val arguments: MutableList<Any> = mutableListOf()
}
