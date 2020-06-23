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
                    if (thisValue is Collection<*> && otherValue is Collection<*> && thisValue.size == otherValue.size) {
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

class Message(
        val id: Identifier,
        val value: Pattern?,
        val attributes: List<Attribute>,
        var comment: Comment? = null
) : Entry()

class Term(
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

class Placeable(val expression: InsidePlaceable) : InsidePlaceable, PatternElement()

abstract class Expression : CallArgument, InsidePlaceable, SyntaxNode()

abstract class Literal(val value: String) : Expression()

class StringLiteral(value: String) : Literal(value)

class NumberLiteral(value: String) : VariantKey, Literal(value)

class MessageReference(val id: Identifier, val attribute: Identifier?) : Expression()

class TermReference(val id: Identifier, val attribute: Identifier?, val arguments: CallArguments?) : Expression()

class VariableReference(val id: Identifier) : Expression()

class FunctionReference(val id: Identifier, val arguments: CallArguments) : Expression()

class SelectExpression(val selector: Expression, val variants: List<Variant>) : Expression()

interface CallArgument

class CallArguments(
        val positional: List<Expression>,
        val named: List<NamedArgument>
) : SyntaxNode()

class Attribute(val id: Identifier, val value: Pattern) : SyntaxNode()

interface VariantKey

class Variant(val key: VariantKey, val value: Pattern, val default: Boolean) : SyntaxNode()

class NamedArgument(val name: Identifier, val value: Literal) : CallArgument, SyntaxNode()

class Identifier(val name: String) : VariantKey, SyntaxNode()

abstract class BaseComment(val content: String) : Entry()

class Comment(content: String) : BaseComment(content)

class GroupComment(content: String) : BaseComment(content)

class ResourceComment(content: String) : BaseComment(content)

class Junk(val content: String, val annotations: List<Annotation>) : TopLevel()

/**
 * Represents top-level whitespace
 *
 * Extension of the data model in other implementations.
 */
class Whitespace(val content: String) : TopLevel()

class Span(val start: Int, val end: Int = start) : BaseNode()

class Annotation(val code: String, val message: String, val arguments: List<Any>, span: Span) : SyntaxNode(span)
